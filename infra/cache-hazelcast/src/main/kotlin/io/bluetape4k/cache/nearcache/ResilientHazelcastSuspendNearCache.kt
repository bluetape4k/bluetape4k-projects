package io.bluetape4k.cache.nearcache

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
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
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient Hazelcast IMap 기반 Near Cache (2-tier: Caffeine front + IMap back) - Coroutine(Suspend) 구현.
 *
 * [HazelcastSuspendNearCache]와 동일한 public API를 유지하면서,
 * IMap back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   IMap 쓰기는 [Channel]에 큐잉하여 consumer coroutine이 순차 처리
 * - **retry**: consumer에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: IMap GET 실패 시 front 값 반환 또는 null
 * - **invalidation**: [HazelcastEntryEventListener]로 타 노드 변경 시 front cache 무효화
 *
 * ```
 * Application (suspend)
 *     |
 * [ResilientHazelcastSuspendNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        Write Channel (Channel<BackCacheCommand>)
 * Caffeine        |
 * (즉시반영)   Consumer Coroutine
 *              (withRetry + imap.setAsync/deleteAsync.await())
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class ResilientHazelcastSuspendNearCache<V: Any>(
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<BackCacheCommand<String, V>>(capacity = config.writeQueueCapacity)

    private val retry: Retry = buildRetry()

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     * remove 후 write-behind 완료 전까지 get/containsKey가 IMap을 읽어 stale 값을 반환하는 것을 방지.
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 IMap read를 차단하는 플래그.
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
        return Retry.of("${config.cacheName}-write-retry", retryConfig)
    }

    private fun launchWriteConsumer() {
        scope.launch {
            for (cmd in writeChannel) {
                try {
                    retry.executeSuspendFunction { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "IMap write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            }
        }
    }

    private suspend fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put -> imap.setAsync(cmd.key, cmd.value).await()
            is BackCacheCommand.PutAll -> cmd.entries.forEach { (k, v) -> imap.setAsync(k, v).await() }
            is BackCacheCommand.Remove -> {
                imap.deleteAsync(cmd.key).await()
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                cmd.keys.forEach { imap.deleteAsync(it).await() }
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                withContext(Dispatchers.IO) { imap.clear() }
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → IMap getAsync → front populate → return
     * - IMap 실패 시 [GetFailureStrategy]에 따라 처리
     */
    suspend fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching { imap.getAsync(key).await() }
                    .onFailure { e -> log.warn(e) { "IMap GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> frontCache.put(key, value) }

            GetFailureStrategy.PROPAGATE_EXCEPTION ->
                imap.getAsync(key).await()?.also { value -> frontCache.put(key, value) }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    suspend fun getAll(keys: Set<String>): Map<String, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        if (missedKeys.isNotEmpty()) {
            missedKeys.forEach { key ->
                runCatching { imap.getAsync(key).await() }
                    .onFailure { e -> log.warn(e) { "IMap GET failed for key=$key during getAll" } }
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
     * - IMap write는 channel로 큐잉 (write-behind)
     */
    suspend fun put(key: String, value: V) {
        key.requireNotBlank("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        writeChannel.trySend(BackCacheCommand.Put(key, value)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping Put for key=$key" }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    suspend fun putAll(map: Map<String, V>) {
        tombstones.removeAll(map.keys)
        frontCache.putAll(map)
        writeChannel.trySend(BackCacheCommand.PutAll(map)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping PutAll for ${map.size} entries" }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    suspend fun putIfAbsent(key: String, value: V): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        val prev = withContext(Dispatchers.IO) { imap.putIfAbsent(key, value) }
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
    suspend fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")
        if (tombstones.contains(key) || clearPending.value) return false

        if (!frontCache.containsKey(key)) {
            if (!withContext(Dispatchers.IO) { imap.containsKey(key) }) return false
        }
        val replaced = withContext(Dispatchers.IO) { imap.replace(key, value) }
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
    suspend fun replace(key: String, oldValue: V, newValue: V): Boolean {
        key.requireNotBlank("key")

        val replaced = withContext(Dispatchers.IO) { imap.replace(key, oldValue, newValue) }
        if (replaced) {
            frontCache.put(key, newValue)
        }
        return replaced
    }

    /**
     * 조회 후 제거한다.
     */
    suspend fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) remove(key)
        return value
    }

    /**
     * 조회 후 새 값으로 교체한다.
     */
    suspend fun getAndReplace(key: String, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or IMap).
     */
    suspend fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return withContext(Dispatchers.IO) { imap.containsKey(key) }
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - IMap delete는 channel로 큐잉 (write-behind)
     */
    suspend fun remove(key: String) {
        key.requireNotBlank("key")
        frontCache.remove(key)
        tombstones.add(key)
        writeChannel.trySend(BackCacheCommand.Remove(key)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping Remove for key=$key" }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    suspend fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        writeChannel.trySend(BackCacheCommand.RemoveAll(keys)).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping RemoveAll for ${keys.size} keys" }
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
    suspend fun clearAll() {
        clearLocal()
        clearPending.value = true
        writeChannel.trySend(BackCacheCommand.ClearBack()).also { result ->
            if (result.isFailure) log.warn { "Write channel full, dropping ClearBack for cacheName=${config.cacheName}" }
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
            runCatching { scope.cancel() }
            runCatching { writeChannel.close() }
            runCatching { imap.removeEntryListener(listenerId) }
            runCatching { frontCache.close() }
            log.debug { "ResilientHazelcastSuspendNearCache [${config.cacheName}] closed" }
        }
    }
}
