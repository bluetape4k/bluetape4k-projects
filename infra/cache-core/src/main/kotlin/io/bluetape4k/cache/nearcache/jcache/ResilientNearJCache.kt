package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.nearcache.GetFailureStrategy

import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotNull
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.atomicfu.atomic
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import javax.cache.Cache

/**
 * Resilient JCache 기반 Near Cache (2-tier: Caffeine front + JCache back) - 동기(Blocking) 구현.
 *
 * JCache back cache와 raw Caffeine front cache를 사용하는 2-tier 캐시.
 * back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   back cache 쓰기는 [LinkedBlockingQueue]에 큐잉하여 daemon thread가 순차 처리
 * - **retry**: consumer thread에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: back cache GET 실패 시 front 값 반환 또는 null
 *
 * ```kotlin
 * Application (blocking)
 *     |
 * [ResilientNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        LinkedBlockingQueue<BackCacheCommand>
 * Caffeine        |
 * (즉시반영)   Daemon Thread (consumer)
 *              (Retry.executeRunnable { backCache.put/remove })
 * ```
 *
 * ```kotlin
 * val config = ResilientNearJCacheConfig<String, Int>(retryMaxAttempts = 3)
 * val nearCache = ResilientNearJCache(backJCache, config)
 * nearCache.put("hello", 5)
 * val value = nearCache.get("hello")
 * // value == 5
 * nearCache.close()
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
class ResilientNearJCache<K: Any, V: Any>(
    private val backCache: Cache<K, V>,
    private val config: ResilientNearJCacheConfig<K, V> = ResilientNearJCacheConfig(),
): AutoCloseable {

    companion object: KLogging()

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: ResilientLocalJCache<K, V> = CaffeineResilientLocalJCache(
        maxLocalSize = config.maxLocalSize,
        frontExpireAfterWrite = config.frontExpireAfterWrite,
        frontExpireAfterAccess = config.frontExpireAfterAccess,
        recordStats = config.recordStats,
    )

    private val retry: Retry = buildRetry()
    private val queue = LinkedBlockingQueue<BackJCacheCommand<K, V>>(config.writeQueueCapacity)

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     */
    private val tombstones: MutableSet<K> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 back cache read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    private val consumerThread: Thread = virtualThread(
        name = "resilient-near-cache-writer-${config.cacheName}",
    ) {
        while (!closed.value) {
            try {
                val cmd = queue.take()
                try {
                    retry.executeRunnable { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "Back cache write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    private fun buildRetry(): Retry {
        val intervalFn = if (config.retryExponentialBackoff) {
            IntervalFunction.ofExponentialBackoff(config.retryWaitDuration, 2.0)
        } else {
            IntervalFunction.of(config.retryWaitDuration)
        }
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(config.retryMaxAttempts)
            .intervalFunction(intervalFn)
            .build()
        return Retry.of("resilient-near-cache-write-retry", retryConfig)
    }

    private fun applyCommand(cmd: BackJCacheCommand<K, V>) {
        when (cmd) {
            is BackJCacheCommand.Put       -> backCache.put(cmd.key, cmd.value)
            is BackJCacheCommand.PutAll    -> backCache.putAll(cmd.entries)
            is BackJCacheCommand.Remove    -> {
                backCache.remove(cmd.key)
                tombstones.remove(cmd.key)
            }
            is BackJCacheCommand.RemoveAll -> {
                cmd.keys.forEach { backCache.remove(it) }
                tombstones.removeAll(cmd.keys)
            }
            is BackJCacheCommand.ClearBack -> {
                backCache.clear()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → back cache GET → front populate → return
     * - back cache 실패 시 [GetFailureStrategy]에 따라 처리
     */
    fun get(key: K): V? {
        key.requireNotNull("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching { backCache.get(key) }
                    .onFailure { e -> log.warn(e) { "Back cache GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> frontCache.put(key, value) }

            GetFailureStrategy.PROPAGATE_EXCEPTION ->
                backCache.get(key)?.also { value -> frontCache.put(key, value) }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    fun getAll(keys: Set<K>): Map<K, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        if (missedKeys.isNotEmpty()) {
            missedKeys.forEach { key ->
                runCatching { backCache.get(key) }
                    .onFailure { e -> log.warn(e) { "Back cache GET failed for key=$key during getAll" } }
                    .getOrNull()
                    ?.let { value ->
                        result[key] = value
                        frontCache.put(key, value)
                    }
            }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - back cache write는 queue로 큐잉 (write-behind)
     */
    fun put(key: K, value: V) {
        key.requireNotNull("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        if (!queue.offer(BackJCacheCommand.Put(key, value))) {
            log.warn { "Write queue full, dropping Put for key=$key" }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    fun putAll(entries: Map<K, V>) {
        tombstones.removeAll(entries.keys)
        frontCache.putAll(entries)
        if (!queue.offer(BackJCacheCommand.PutAll(entries))) {
            log.warn { "Write queue full, dropping PutAll for ${entries.size} entries" }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: K, value: V): V? {
        key.requireNotNull("key")

        val existing = get(key)
        if (existing != null) return existing

        val prev = backCache.getAndPut(key, value)
        return if (prev == null) {
            frontCache.put(key, value)
            null
        } else {
            prev
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    fun replace(key: K, value: V): Boolean {
        key.requireNotNull("key")
        if (tombstones.contains(key) || clearPending.value) return false

        if (!frontCache.containsKey(key)) {
            if (!runCatching { backCache.containsKey(key) }.getOrDefault(false)) return false
        }
        val replaced = backCache.replace(key, value)
        if (replaced) {
            frontCache.put(key, value)
        }
        return replaced
    }

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    fun replace(key: K, oldValue: V, newValue: V): Boolean {
        val current = get(key) ?: return false
        if (current != oldValue) return false
        return replace(key, newValue)
    }

    /**
     * 조회 후 제거한다.
     */
    fun getAndRemove(key: K): V? {
        val value = get(key)
        if (value != null) remove(key)
        return value
    }

    /**
     * 조회 후 새 값으로 교체한다.
     */
    fun getAndReplace(key: K, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or back).
     */
    fun containsKey(key: K): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return runCatching { backCache.containsKey(key) }
            .onFailure { e -> log.warn(e) { "Back cache containsKey failed for key=$key" } }
            .getOrDefault(false)
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - back cache delete는 queue로 큐잉 (write-behind)
     */
    fun remove(key: K) {
        key.requireNotNull("key")
        frontCache.remove(key)
        tombstones.add(key)
        if (!queue.offer(BackJCacheCommand.Remove(key))) {
            log.warn { "Write queue full, dropping Remove for key=$key" }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    fun removeAll(keys: Set<K>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        if (!queue.offer(BackJCacheCommand.RemoveAll(keys))) {
            log.warn { "Write queue full, dropping RemoveAll for ${keys.size} keys" }
        }
    }

    /**
     * 로컬 캐시만 비운다 (back cache 유지).
     */
    fun clearLocal() {
        frontCache.clear()
        log.debug { "Front cache cleared" }
    }

    /**
     * 로컬 캐시와 back cache를 모두 비운다 (write-behind).
     */
    fun clearAll() {
        clearLocal()
        clearPending.value = true
        if (!queue.offer(BackJCacheCommand.ClearBack())) {
            log.warn { "Write queue full, dropping ClearBack" }
        }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * 모든 리소스를 정리한다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { consumerThread.interrupt() }
            runCatching { queue.clear() }
            runCatching { frontCache.close() }
            log.debug { "ResilientNearCache closed" }
        }
    }
}
