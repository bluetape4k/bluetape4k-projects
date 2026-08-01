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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import kotlin.concurrent.withLock

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
 * write-behind mutation 메서드는 [CompletableFuture]를 반환한다. future가 정상 완료되면
 * back cache 반영까지 끝난 것이고, 재시도 소진 또는 close drain timeout은 예외 완료된다.
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
 * nearCache.put("hello", 5).join()
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

    private val closeDrainTimeoutMillis: Long = config.closeDrainTimeout.toMillis().coerceAtLeast(1L)

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: ResilientLocalJCache<K, V> = CaffeineResilientLocalJCache(
        maxLocalSize = config.maxLocalSize,
        frontExpireAfterWrite = config.frontExpireAfterWrite,
        frontExpireAfterAccess = config.frontExpireAfterAccess,
        recordStats = config.recordStats,
    )

    private val retry: Retry = buildRetry()
    private val queue = LinkedBlockingQueue<QueuedCommand<K, V>>(config.writeQueueCapacity)
    private val writeStateLock = ReentrantLock()
    private val pendingCommands = ConcurrentHashMap.newKeySet<QueuedCommand<K, V>>()
    private val pendingMutationTokens = ConcurrentHashMap<K, MutationToken<V>>()
    private val pendingClearToken = atomic<ClearToken?>(null)
    private val stateVersion = atomic(0L)
    private var nextCommandSequence = 0L

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
        while (true) {
            try {
                val cmd = if (closed.value) {
                    queue.poll() ?: break
                } else {
                    queue.take()
                }
                writeStateLock.withLock { }
                try {
                    retry.executeRunnable { applyCommand(cmd.command) }
                    writeStateLock.withLock {
                        completeCommand(cmd, failed = false)
                    }
                } catch (e: Exception) {
                    log.error(e) {
                        "Back cache write failed after ${config.retryMaxAttempts} retries, compensating command: ${cmd.command}"
                    }
                    writeStateLock.withLock {
                        completeCommand(cmd, failed = true, failure = e)
                    }
                }
            } catch (e: InterruptedException) {
                if (closed.value) {
                    continue
                } else {
                    Thread.currentThread().interrupt()
                    break
                }
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
            is BackJCacheCommand.Remove    -> backCache.remove(cmd.key)
            is BackJCacheCommand.RemoveAll -> cmd.keys.forEach { backCache.remove(it) }
            is BackJCacheCommand.ClearBack -> backCache.clear()
        }
    }

    private fun completeCommand(queued: QueuedCommand<K, V>, failed: Boolean, failure: Throwable? = null) {
        if (!queued.completed.compareAndSet(false, true)) return
        when (val command = queued.command) {
            is BackJCacheCommand.Put       -> completePut(queued, setOf(command.key), failed)
            is BackJCacheCommand.PutAll    -> completePut(queued, command.entries.keys, failed)
            is BackJCacheCommand.Remove    -> completeRemove(queued, setOf(command.key), failed)
            is BackJCacheCommand.RemoveAll -> completeRemove(queued, command.keys, failed)
            is BackJCacheCommand.ClearBack -> queued.clearToken?.let { token ->
                if (!failed && pendingClearToken.compareAndSet(token, null)) {
                    stateVersion.incrementAndGet()
                    clearPending.value = false
                }
            }
        }
        if (failed) {
            queued.completion.completeExceptionally(
                failure ?: IllegalStateException("Back cache write failed for ${queued.command}")
            )
        } else {
            queued.completion.complete(Unit)
        }
        pendingCommands.remove(queued)
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

    private fun completeRemove(queued: QueuedCommand<K, V>, keys: Set<K>, failed: Boolean) {
        keys.forEach { key ->
            val token = queued.mutationTokens.getValue(key)
            if (pendingMutationTokens.remove(key, token) && !failed) {
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
    fun get(key: K): V? {
        key.requireNotNull("key")

        val observedVersion = writeStateLock.withLock {
            if (tombstones.contains(key) || clearPending.value) return null
            frontCache.get(key)?.let { return it }
            stateVersion.value
        }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching { backCache.get(key) }
                    .onFailure { e -> log.warn(e) { "Back cache GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> populateFrontIfUnchanged(key, value, observedVersion) }

            GetFailureStrategy.PROPAGATE_EXCEPTION ->
                backCache.get(key)?.also { value -> populateFrontIfUnchanged(key, value, observedVersion) }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    fun getAll(keys: Set<K>): Map<K, V> {
        val (result, missedKeys, observedVersion) = writeStateLock.withLock {
            if (clearPending.value) return emptyMap()
            val frontResult = frontCache.getAll(keys).toMutableMap()
            Triple(
                frontResult,
                (keys - frontResult.keys).filter { !tombstones.contains(it) },
                stateVersion.value,
            )
        }

        if (missedKeys.isNotEmpty()) {
            missedKeys.forEach { key ->
                runCatching { backCache.get(key) }
                    .onFailure { e -> log.warn(e) { "Back cache GET failed for key=$key during getAll" } }
                    .getOrNull()
                    ?.let { value ->
                        result[key] = value
                        populateFrontIfUnchanged(key, value, observedVersion)
                    }
            }
        }
        return result
    }

    private fun populateFrontIfUnchanged(key: K, value: V, observedVersion: Long) {
        writeStateLock.withLock {
            if (stateVersion.value == observedVersion && !tombstones.contains(key) && !clearPending.value) {
                frontCache.put(key, value)
            }
        }
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - back cache write는 queue로 큐잉 (write-behind)
     *
     * @return back cache 반영 완료 future. 재시도 소진 또는 close drain timeout이면
     *   exceptionally completed 상태가 된다. 큐가 가득 차면 front cache를 변경하기 전에
     *   [IllegalStateException]을 즉시 던진다.
     */
    fun put(key: K, value: V): CompletableFuture<Unit> {
        key.requireNotNull("key")
        return enqueueWrite(BackJCacheCommand.Put(key, value), "Write queue full for Put key=$key") {
            tombstones.remove(key)
            frontCache.put(key, value)
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     *
     * @return 모든 항목의 back cache 반영 완료 future. 재시도 소진 또는 close drain timeout이면
     *   exceptionally completed 상태가 된다.
     */
    fun putAll(entries: Map<K, V>): CompletableFuture<Unit> {
        val entriesSnapshot = entries.toMap()
        return enqueueWrite(
            BackJCacheCommand.PutAll(entriesSnapshot),
            "Write queue full for PutAll entries=${entries.size}"
        ) {
            tombstones.removeAll(entriesSnapshot.keys)
            frontCache.putAll(entriesSnapshot)
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: K, value: V): V? {
        key.requireNotNull("key")
        return writeStateLock.withLock {
            frontCache.get(key)?.let { return it }
            val pendingMutation = pendingMutationTokens[key]
            val pendingClear = pendingClearToken.value
            if (
                pendingMutation?.value != null &&
                (pendingClear == null || pendingMutation.sequence > pendingClear.sequence)
            ) {
                return pendingMutation.value
            }

            if (pendingMutation != null || pendingClear != null) {
                enqueueWriteLocked(BackJCacheCommand.Put(key, value), "Write queue full for Put key=$key") {
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
    fun replace(key: K, value: V): Boolean {
        key.requireNotNull("key")
        return writeStateLock.withLock {
            if (pendingMutationTokens.containsKey(key) || tombstones.contains(key) || clearPending.value) return false

            if (!frontCache.containsKey(key)) {
                if (!runCatching { backCache.containsKey(key) }.getOrDefault(false)) return false
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
     *
     * @return back cache 삭제 완료 future. 재시도 소진 또는 close drain timeout이면
     *   exceptionally completed 상태가 되며, stale back read를 막는 tombstone은 유지된다.
     */
    fun remove(key: K): CompletableFuture<Unit> {
        key.requireNotNull("key")
        return enqueueWrite(BackJCacheCommand.Remove(key), "Write queue full for Remove key=$key") {
            frontCache.remove(key)
            tombstones.add(key)
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     *
     * @return 모든 키의 back cache 삭제 완료 future. 실패 시 각 키의 tombstone은 유지된다.
     */
    fun removeAll(keys: Set<K>): CompletableFuture<Unit> {
        val keysSnapshot = keys.toSet()
        return enqueueWrite(
            BackJCacheCommand.RemoveAll(keysSnapshot),
            "Write queue full for RemoveAll keys=${keys.size}"
        ) {
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
     *
     * @return back cache clear 완료 future. 실패 또는 close drain timeout이면 clear pending
     *   상태와 stale read 차단을 유지한 채 exceptionally completed 상태가 된다.
     */
    fun clearAll(): CompletableFuture<Unit> {
        return enqueueWrite(BackJCacheCommand.ClearBack(), "Write queue full for ClearBack") {
            clearPending.value = true
            clearLocal()
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
        val shouldClose = writeStateLock.withLock {
            closed.compareAndSet(false, true).also { changed ->
                if (changed) {
                    runCatching { consumerThread.interrupt() }
                }
            }
        }
        if (shouldClose) {
            runCatching {
                if (Thread.currentThread() != consumerThread) {
                    consumerThread.join(closeDrainTimeoutMillis)
                    if (consumerThread.isAlive) {
                        log.warn { "Timed out draining back cache write queue. remaining=${queue.size}" }
                        failPendingCommands(IllegalStateException("Timed out draining back cache write queue"))
                    }
                }
            }
            runCatching { frontCache.close() }
            log.debug { "ResilientNearCache closed" }
        }
    }

    private fun enqueueWrite(
        command: BackJCacheCommand<K, V>,
        failureMessage: String,
        updateFrontState: () -> Unit,
    ): CompletableFuture<Unit> {
        writeStateLock.withLock {
            return enqueueWriteLocked(command, failureMessage, updateFrontState)
        }
    }

    private fun enqueueWriteLocked(
        command: BackJCacheCommand<K, V>,
        failureMessage: String,
        updateFrontState: () -> Unit,
    ): CompletableFuture<Unit> {
        val queued = QueuedCommand(command, ++nextCommandSequence)
        check(!closed.value) { "ResilientNearCache is closed" }
        pendingCommands.add(queued)
        if (!queue.offer(queued)) {
            pendingCommands.remove(queued)
            check(false) { failureMessage }
        }
        queued.mutationTokens.forEach(pendingMutationTokens::put)
        queued.clearToken?.let { pendingClearToken.value = it }
        stateVersion.incrementAndGet()
        updateFrontState()
        return queued.completion
    }

    private fun failPendingCommands(failure: Throwable) {
        pendingCommands.toList().forEach { queued ->
            completeCommand(queued, failed = true, failure = failure)
        }
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
        val completion: CompletableFuture<Unit> = CompletableFuture()
        val completed = atomic(false)

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
