package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotNull
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient SuspendCache 기반 Near Cache (2-tier: Caffeine front + SuspendCache back) - Coroutine(Suspend) 구현.
 *
 * [SuspendCache] back cache와 raw Caffeine front cache를 사용하는 2-tier 캐시.
 * back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   back cache 쓰기는 [Channel]에 큐잉하여 consumer coroutine이 순차 처리
 * - **retry**: consumer에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: back cache GET 실패 시 front 값 반환 또는 null
 *
 * ```
 * Application (suspend)
 *     |
 * [ResilientSuspendNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        Write Channel (Channel<BackCacheCommand>)
 * Caffeine        |
 * (즉시반영)   Consumer Coroutine
 *              (withRetry + backCache.put/remove)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
class ResilientSuspendNearCache<K: Any, V: Any>(
    private val backCache: SuspendCache<K, V>,
    private val config: ResilientNearCacheConfig<K, V> = ResilientNearCacheConfig(),
): AutoCloseable {

    companion object: KLogging()

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: ResilientLocalCache<K, V> = CaffeineResilientLocalCache(
        maxLocalSize = config.maxLocalSize,
        frontExpireAfterWrite = config.frontExpireAfterWrite,
        frontExpireAfterAccess = config.frontExpireAfterAccess,
        recordStats = config.recordStats,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<BackCacheCommand<K, V>>(capacity = config.writeQueueCapacity)

    private val retry: Retry = buildRetry()

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     */
    private val tombstones: MutableSet<K> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 back cache read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    init {
        launchWriteConsumer()
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
        return Retry.of("resilient-suspend-near-cache-write-retry", retryConfig)
    }

    private fun launchWriteConsumer() {
        scope.launch {
            for (cmd in writeChannel) {
                try {
                    retry.executeSuspendFunction { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "Back cache write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            }
        }
    }

    private suspend fun applyCommand(cmd: BackCacheCommand<K, V>) {
        when (cmd) {
            is BackCacheCommand.Put -> backCache.put(cmd.key, cmd.value)
            is BackCacheCommand.PutAll -> backCache.putAll(cmd.entries)
            is BackCacheCommand.Remove -> {
                backCache.remove(cmd.key)
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                backCache.removeAll(cmd.keys)
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
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
    suspend fun get(key: K): V? {
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
    suspend fun getAll(keys: Set<K>): Map<K, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        missedKeys.forEach { key ->
            runCatching { backCache.get(key) }
                .onFailure { e -> log.warn(e) { "Back cache GET failed for key=$key during getAll" } }
                .getOrNull()
                ?.let { value ->
                    result[key] = value
                    frontCache.put(key, value)
                }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - back cache write는 channel로 큐잉 (write-behind)
     */
    suspend fun put(key: K, value: V) {
        key.requireNotNull("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        writeChannel.trySend(BackCacheCommand.Put(key, value)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping Put for key=$key" }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    suspend fun putAll(entries: Map<K, V>) {
        tombstones.removeAll(entries.keys)
        frontCache.putAll(entries)
        writeChannel.trySend(BackCacheCommand.PutAll(entries)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping PutAll for ${entries.size} entries" }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    suspend fun putIfAbsent(key: K, value: V): V? {
        key.requireNotNull("key")

        val existing = get(key)
        if (existing != null) return existing

        val setted = backCache.putIfAbsent(key, value)
        return if (setted) {
            frontCache.put(key, value)
            null
        } else {
            backCache.get(key)
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    suspend fun replace(key: K, value: V): Boolean {
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
    suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        val current = get(key) ?: return false
        if (current != oldValue) return false
        return replace(key, newValue)
    }

    /**
     * 조회 후 제거한다.
     */
    suspend fun getAndRemove(key: K): V? {
        val value = get(key)
        if (value != null) remove(key)
        return value
    }

    /**
     * 조회 후 새 값으로 교체한다.
     */
    suspend fun getAndReplace(key: K, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or back).
     */
    suspend fun containsKey(key: K): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return runCatching { backCache.containsKey(key) }
            .onFailure { e -> log.warn(e) { "Back cache containsKey failed for key=$key" } }
            .getOrDefault(false)
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - back cache delete는 channel로 큐잉 (write-behind)
     */
    suspend fun remove(key: K) {
        key.requireNotNull("key")
        frontCache.remove(key)
        tombstones.add(key)
        writeChannel.trySend(BackCacheCommand.Remove(key)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping Remove for key=$key" }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    suspend fun removeAll(keys: Set<K>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        writeChannel.trySend(BackCacheCommand.RemoveAll(keys)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping RemoveAll for ${keys.size} keys" }
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
    suspend fun clearAll() {
        clearLocal()
        clearPending.value = true
        writeChannel.trySend(BackCacheCommand.ClearBack()).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping ClearBack" }
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
            runCatching { scope.cancel() }
            runCatching { writeChannel.close() }
            runCatching { frontCache.close() }
            log.debug { "ResilientSuspendNearCache closed" }
        }
    }
}
