package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.cache.nearcache.GetFailureStrategy
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotNull
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient SuspendCache 기반 Near Cache (2-tier: Caffeine front + SuspendCache back) - Coroutine(Suspend) 구현.
 *
 * [SuspendJCache] back cache와 raw Caffeine front cache를 사용하는 2-tier 캐시.
 * back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   back cache 쓰기는 [Channel]에 큐잉하여 consumer coroutine이 순차 처리
 * - **retry**: consumer에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: back cache GET 실패 시 front 값 반환 또는 null
 *
 * ```kotlin
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
 * ```kotlin
 * val config = ResilientNearJCacheConfig<String, Int>(retryMaxAttempts = 3)
 * val nearCache = ResilientSuspendNearJCache(backSuspendCache, config)
 * nearCache.put("hello", 5)
 * val value = nearCache.get("hello")
 * // value == 5
 * nearCache.close()
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
class ResilientSuspendNearJCache<K: Any, V: Any>(
    private val backCache: SuspendJCache<K, V>,
    private val config: ResilientNearJCacheConfig<K, V> = ResilientNearJCacheConfig(),
): AutoCloseable {

    companion object: KLogging()

    private val closeDrainTimeoutMillis: Long = 5_000L

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: ResilientLocalJCache<K, V> = CaffeineResilientLocalJCache(
        maxLocalSize = config.maxLocalSize,
        frontExpireAfterWrite = config.frontExpireAfterWrite,
        frontExpireAfterAccess = config.frontExpireAfterAccess,
        recordStats = config.recordStats,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<QueuedCommand<K, V>>(capacity = config.writeQueueCapacity)
    private val writeStateMutex = Mutex()
    private val pendingMutationTokens = ConcurrentHashMap<K, MutationToken<V>>()
    private val pendingClearToken = atomic<ClearToken?>(null)
    private val stateVersion = atomic(0L)
    private var nextCommandSequence = 0L

    private val retry: Retry = buildRetry()

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     */
    private val tombstones: MutableSet<K> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 back cache read를 차단하는 플래그.
     */
    private val clearPending = atomic(false)

    private val writeConsumerJob: Job = launchWriteConsumer()

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

    private fun launchWriteConsumer(): Job =
        scope.launch {
            for (cmd in writeChannel) {
                writeStateMutex.withLock { }
                try {
                    retry.executeSuspendFunction { applyCommand(cmd.command) }
                    writeStateMutex.withLock {
                        completeCommand(cmd, failed = false)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error(e) {
                        "Back cache write failed after ${config.retryMaxAttempts} retries, compensating command: ${cmd.command}"
                    }
                    writeStateMutex.withLock {
                        completeCommand(cmd, failed = true)
                    }
                }
            }
        }

    private suspend fun applyCommand(cmd: BackJCacheCommand<K, V>) {
        when (cmd) {
            is BackJCacheCommand.Put       -> backCache.put(cmd.key, cmd.value)
            is BackJCacheCommand.PutAll    -> backCache.putAll(cmd.entries)
            is BackJCacheCommand.Remove    -> backCache.remove(cmd.key)
            is BackJCacheCommand.RemoveAll -> backCache.removeAll(cmd.keys)
            is BackJCacheCommand.ClearBack -> backCache.clear()
        }
    }

    private fun completeCommand(queued: QueuedCommand<K, V>, failed: Boolean) {
        when (val command = queued.command) {
            is BackJCacheCommand.Put       -> completePut(queued, setOf(command.key), failed)
            is BackJCacheCommand.PutAll    -> completePut(queued, command.entries.keys, failed)
            is BackJCacheCommand.Remove    -> completeRemove(queued, setOf(command.key))
            is BackJCacheCommand.RemoveAll -> completeRemove(queued, command.keys)
            is BackJCacheCommand.ClearBack -> queued.clearToken?.let { token ->
                if (pendingClearToken.compareAndSet(token, null)) {
                    stateVersion.incrementAndGet()
                    clearPending.value = false
                }
            }
        }
    }

    private fun completePut(queued: QueuedCommand<K, V>, keys: Set<K>, failed: Boolean) {
        keys.forEach { key ->
            val token = queued.mutationTokens.getValue(key)
            if (pendingMutationTokens.remove(key, token) && failed) {
                stateVersion.incrementAndGet()
                frontCache.remove(key)
            }
        }
    }

    private fun completeRemove(queued: QueuedCommand<K, V>, keys: Set<K>) {
        keys.forEach { key ->
            val token = queued.mutationTokens.getValue(key)
            if (pendingMutationTokens.remove(key, token)) {
                stateVersion.incrementAndGet()
                tombstones.remove(key)
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

        var frontValue: V? = null
        val observedVersion = writeStateMutex.withLock {
            if (tombstones.contains(key) || clearPending.value) return null
            frontValue = frontCache.get(key)
            stateVersion.value
        }
        frontValue?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL -> {
                val value = getBackOrNull(key, "Back cache GET failed for key=$key, returning null")
                value?.let { populateFrontIfUnchanged(key, it, observedVersion) }
                value
            }

            GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                val value = backCache.get(key)
                value?.let { populateFrontIfUnchanged(key, it, observedVersion) }
                value
            }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    suspend fun getAll(keys: Set<K>): Map<K, V> {
        val (result, missedKeys, observedVersion) = writeStateMutex.withLock {
            if (clearPending.value) return emptyMap()
            val frontResult = frontCache.getAll(keys).toMutableMap()
            Triple(
                frontResult,
                (keys - frontResult.keys).filter { !tombstones.contains(it) },
                stateVersion.value,
            )
        }

        missedKeys.forEach { key ->
            getBackOrNull(key, "Back cache GET failed for key=$key during getAll")
                ?.let { value ->
                    result[key] = value
                    populateFrontIfUnchanged(key, value, observedVersion)
                }
        }
        return result
    }

    private suspend fun populateFrontIfUnchanged(key: K, value: V, observedVersion: Long) {
        writeStateMutex.withLock {
            if (stateVersion.value == observedVersion && !tombstones.contains(key) && !clearPending.value) {
                frontCache.put(key, value)
            }
        }
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - back cache write는 channel로 큐잉 (write-behind)
     */
    suspend fun put(key: K, value: V) {
        key.requireNotNull("key")
        enqueueWrite(BackJCacheCommand.Put(key, value), "Write channel full for Put key=$key") {
            tombstones.remove(key)
            frontCache.put(key, value)
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    suspend fun putAll(entries: Map<K, V>) {
        val entriesSnapshot = entries.toMap()
        enqueueWrite(BackJCacheCommand.PutAll(entriesSnapshot), "Write channel full for PutAll entries=${entries.size}") {
            tombstones.removeAll(entriesSnapshot.keys)
            frontCache.putAll(entriesSnapshot)
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    suspend fun putIfAbsent(key: K, value: V): V? {
        key.requireNotNull("key")
        return writeStateMutex.withLock {
            frontCache.get(key)?.let { return it }
            val pendingMutation = pendingMutationTokens[key]
            val pendingClear = pendingClearToken.value
            if (pendingMutation?.value != null && (pendingClear == null || pendingMutation.sequence > pendingClear.sequence)) {
                return pendingMutation.value
            }

            if (pendingMutation != null || pendingClear != null) {
                enqueueWriteLocked(BackJCacheCommand.Put(key, value), "Write channel full for Put key=$key") {
                    tombstones.remove(key)
                    frontCache.put(key, value)
                }
                return null
            }

            if (backCache.putIfAbsent(key, value)) {
                stateVersion.incrementAndGet()
                frontCache.put(key, value)
                null
            } else {
                backCache.get(key)
            }
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    suspend fun replace(key: K, value: V): Boolean {
        key.requireNotNull("key")
        return writeStateMutex.withLock {
            if (pendingMutationTokens.containsKey(key) || tombstones.contains(key) || clearPending.value) return false

            if (!frontCache.containsKey(key)) {
                if (!containsBackOrFalse(key, null)) return false
            }
            backCache.replace(key, value).also { replaced ->
                if (replaced) {
                    stateVersion.incrementAndGet()
                    frontCache.put(key, value)
                }
            }
        }
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
        return containsBackOrFalse(key, "Back cache containsKey failed for key=$key")
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - back cache delete는 channel로 큐잉 (write-behind)
     */
    suspend fun remove(key: K) {
        key.requireNotNull("key")
        enqueueWrite(BackJCacheCommand.Remove(key), "Write channel full for Remove key=$key") {
            frontCache.remove(key)
            tombstones.add(key)
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    suspend fun removeAll(keys: Set<K>) {
        val keysSnapshot = keys.toSet()
        enqueueWrite(BackJCacheCommand.RemoveAll(keysSnapshot), "Write channel full for RemoveAll keys=${keys.size}") {
            frontCache.removeAll(keysSnapshot)
            tombstones.addAll(keysSnapshot)
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
        enqueueWrite(BackJCacheCommand.ClearBack(), "Write channel full for ClearBack") {
            clearPending.value = true
            clearLocal()
        }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    private suspend fun getBackOrNull(
        key: K,
        failureMessage: String,
    ): V? =
        try {
            backCache.get(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { failureMessage }
            null
        }

    private suspend fun containsBackOrFalse(
        key: K,
        failureMessage: String?,
    ): Boolean =
        try {
            backCache.containsKey(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (failureMessage != null) {
                log.warn(e) { failureMessage }
            }
            false
        }

    /**
     * 모든 리소스를 정리한다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { writeChannel.close() }
            runCatching {
                runBlocking {
                    val drained = withTimeoutOrNull(closeDrainTimeoutMillis) {
                        writeConsumerJob.join()
                        true
                    } ?: false
                    if (!drained) {
                        log.warn { "Timed out draining back cache write channel." }
                    }
                }
            }
            runCatching { scope.cancel() }
            runCatching { frontCache.close() }
            log.debug { "ResilientSuspendNearCache closed" }
        }
    }

    private suspend fun enqueueWrite(
        command: BackJCacheCommand<K, V>,
        failureMessage: String,
        updateFrontState: () -> Unit,
    ) {
        writeStateMutex.withLock {
            enqueueWriteLocked(command, failureMessage, updateFrontState)
        }
    }

    private fun enqueueWriteLocked(
        command: BackJCacheCommand<K, V>,
        failureMessage: String,
        updateFrontState: () -> Unit,
    ) {
        val queued = QueuedCommand(command, ++nextCommandSequence)
        check(!closed.value) { "ResilientSuspendNearCache is closed" }
        check(writeChannel.trySend(queued).isSuccess) { failureMessage }
        queued.mutationTokens.forEach(pendingMutationTokens::put)
        queued.clearToken?.let { pendingClearToken.value = it }
        stateVersion.incrementAndGet()
        updateFrontState()
    }

    private class MutationToken<V: Any>(
        val value: V?,
        val sequence: Long,
    )

    private class ClearToken(val sequence: Long)

    private class QueuedCommand<K: Any, V: Any>(
        val command: BackJCacheCommand<K, V>,
        sequence: Long,
    ) {
        val mutationTokens: Map<K, MutationToken<V>> = when (command) {
            is BackJCacheCommand.Put       -> mapOf(command.key to MutationToken(command.value, sequence))
            is BackJCacheCommand.PutAll    -> command.entries.mapValues { MutationToken(it.value, sequence) }
            is BackJCacheCommand.Remove    -> mapOf(command.key to MutationToken(null, sequence))
            is BackJCacheCommand.RemoveAll -> command.keys.associateWith { MutationToken(null, sequence) }
            is BackJCacheCommand.ClearBack -> emptyMap()
        }

        val clearToken: ClearToken? = if (command is BackJCacheCommand.ClearBack) ClearToken(sequence) else null
    }
}
