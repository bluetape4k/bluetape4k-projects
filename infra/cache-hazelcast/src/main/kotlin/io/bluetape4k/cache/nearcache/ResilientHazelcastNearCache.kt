package io.bluetape4k.cache.nearcache

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.atomicfu.atomic
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * Resilient Hazelcast IMap 기반 Near Cache (2-tier: Caffeine front + IMap back) - 동기(Blocking) 구현.
 *
 * [HazelcastNearCache]와 동일한 public API를 유지하면서,
 * IMap back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   IMap 쓰기는 [LinkedBlockingQueue]에 큐잉하여 daemon thread가 순차 처리
 * - **retry**: consumer thread에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: IMap GET 실패 시 front 값 반환 또는 null
 * - **invalidation**: [HazelcastEntryEventListener]로 타 노드 변경 시 front cache 무효화
 *
 * ```
 * Application (blocking)
 *     |
 * [ResilientHazelcastNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        LinkedBlockingQueue<BackCacheCommand>
 * Caffeine        |
 * (즉시반영)   Daemon Thread (consumer)
 *              (Retry.executeRunnable { imap.set/delete })
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class ResilientHazelcastNearCache<V: Any>(
    hazelcastInstance: HazelcastInstance,
    private val config: ResilientHazelcastNearCacheConfig = ResilientHazelcastNearCacheConfig(
        HazelcastNearCacheConfig()
    ),
): AutoCloseable {

    companion object: KLogging()

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    @Suppress("UNCHECKED_CAST")
    private val imap: IMap<String, V> = hazelcastInstance.getMap(config.cacheName)
    private val frontCache: HazelcastLocalCache<String, V> = CaffeineHazelcastLocalCache(config.base)

    private val entryListener = HazelcastEntryEventListener(frontCache)
    private val listenerId: UUID = imap.addEntryListener(entryListener, true)

    private val retry: Retry = buildRetry()
    private val queue = LinkedBlockingQueue<BackCacheCommand<String, V>>(config.writeQueueCapacity)

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     * remove 후 write-behind 완료 전까지 get/containsKey가 IMap을 읽어 stale 값을 반환하는 것을 방지.
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 IMap read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    private val consumerThread: Thread = virtualThread(
        name = "resilient-hazelcast-writer-${config.cacheName}",
    ) {
        while (!closed.value) {
            try {
                val cmd = queue.take()
                try {
                    retry.executeRunnable { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "IMap write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
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
        return Retry.of("${config.cacheName}-write-retry", retryConfig)
    }

    private fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put -> imap.set(cmd.key, cmd.value)
            is BackCacheCommand.PutAll -> imap.putAll(cmd.entries)
            is BackCacheCommand.Remove -> {
                imap.delete(cmd.key)
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                cmd.keys.forEach { imap.delete(it) }
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                imap.clear()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → IMap GET → front populate → return
     * - IMap 실패 시 [GetFailureStrategy]에 따라 처리
     */
    fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching { imap.get(key) }
                    .onFailure { e -> log.warn(e) { "IMap GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> frontCache.put(key, value) }

            GetFailureStrategy.PROPAGATE_EXCEPTION ->
                imap.get(key)?.also { value -> frontCache.put(key, value) }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    fun getAll(keys: Set<String>): Map<String, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }.toSet()

        if (missedKeys.isNotEmpty()) {
            runCatching { imap.getAll(missedKeys) }
                .onFailure { e -> log.warn(e) { "IMap getAll failed, returning partial result" } }
                .getOrNull()
                ?.forEach { (key, value) ->
                    result[key] = value
                    frontCache.put(key, value)
                }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - IMap write는 queue로 큐잉 (write-behind)
     */
    fun put(key: String, value: V) {
        key.requireNotBlank("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        if (!queue.offer(BackCacheCommand.Put(key, value))) {
            log.warn { "Write queue full, dropping Put for key=$key" }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    fun putAll(map: Map<String, V>) {
        tombstones.removeAll(map.keys)
        frontCache.putAll(map)
        if (!queue.offer(BackCacheCommand.PutAll(map))) {
            log.warn { "Write queue full, dropping PutAll for ${map.size} entries" }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * IMap.putIfAbsent()를 직접 사용하여 원자성을 보장한다.
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: String, value: V): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        val prev = imap.putIfAbsent(key, value)
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
    fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")
        if (tombstones.contains(key) || clearPending.value) return false

        if (!frontCache.containsKey(key)) {
            if (!runCatching { imap.containsKey(key) }.getOrDefault(false)) return false
        }
        val replaced = imap.replace(key, value)
        return if (replaced != null) {
            frontCache.put(key, value)
            true
        } else {
            false
        }
    }

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    fun replace(key: String, oldValue: V, newValue: V): Boolean {
        key.requireNotBlank("key")

        val replaced = imap.replace(key, oldValue, newValue)
        if (replaced) {
            frontCache.put(key, newValue)
        }
        return replaced
    }

    /**
     * 조회 후 제거한다.
     */
    fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) remove(key)
        return value
    }

    /**
     * 조회 후 새 값으로 교체한다.
     */
    fun getAndReplace(key: String, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or IMap).
     */
    fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return runCatching { imap.containsKey(key) }
            .onFailure { e -> log.warn(e) { "IMap containsKey failed for key=$key" } }
            .getOrDefault(false)
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - IMap delete는 queue로 큐잉 (write-behind)
     */
    fun remove(key: String) {
        key.requireNotBlank("key")
        frontCache.remove(key)
        tombstones.add(key)
        if (!queue.offer(BackCacheCommand.Remove(key))) {
            log.warn { "Write queue full, dropping Remove for key=$key" }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        if (!queue.offer(BackCacheCommand.RemoveAll(keys))) {
            log.warn { "Write queue full, dropping RemoveAll for ${keys.size} keys" }
        }
    }

    /**
     * 로컬 캐시만 비운다 (IMap 유지).
     */
    fun clearLocal() {
        frontCache.clear()
        log.debug { "Front cache cleared for cacheName=${config.cacheName}" }
    }

    /**
     * 로컬 캐시와 IMap을 모두 비운다 (write-behind).
     */
    fun clearAll() {
        clearLocal()
        clearPending.value = true
        if (!queue.offer(BackCacheCommand.ClearBack())) {
            log.warn { "Write queue full, dropping ClearBack for cacheName=${config.cacheName}" }
        }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * IMap(back-cache)의 크기.
     */
    fun backCacheSize(): Long = imap.size.toLong()

    /**
     * 모든 리소스를 정리하고 리스너를 제거한다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { consumerThread.interrupt() }
            runCatching { queue.clear() }
            runCatching { imap.removeEntryListener(listenerId) }
            runCatching { frontCache.close() }
            log.debug { "ResilientHazelcastNearCache [${config.cacheName}] closed" }
        }
    }
}
