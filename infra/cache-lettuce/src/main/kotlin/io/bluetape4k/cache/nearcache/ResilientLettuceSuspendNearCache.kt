package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.lettuceDefaultCodec
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.resilience4j.retry.withRetry
import io.bluetape4k.support.requireNotBlank
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.MSetExArgs
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.codec.RedisCodec
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient Lettuce Suspend Near Cache (2-tier: Caffeine front + Redis back).
 *
 * [LettuceSuspendNearCache]와 동일한 public API를 유지하면서,
 * Redis back cache 호출에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   Redis 쓰기는 [Channel]에 큐잉하여 consumer coroutine이 순차 처리
 * - **retry**: consumer에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: Redis GET 실패 시 front 값 반환 또는 null
 *
 * ```
 * Application (suspend)
 *     |
 * [ResilientLettuceSuspendNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        Write Channel (Channel<BackCacheCommand>)
 * Caffeine        |
 * (즉시반영)   Consumer Coroutine
 *              (withRetry + Redis SET/DEL)
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class ResilientLettuceSuspendNearCache<V: Any>(
    private val redisClient: RedisClient,
    private val codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    private val config: ResilientLettuceNearCacheConfig<String, V> = ResilientLettuceNearCacheConfig(
        LettuceNearCacheConfig()
    ),
): AutoCloseable {

    companion object: KLogging() {
        /**
         * String 키/값 타입의 Resilient Near Suspend Cache를 생성한다.
         */
        operator fun invoke(
            redisClient: RedisClient,
            config: ResilientLettuceNearCacheConfig<String, String> = ResilientLettuceNearCacheConfig(
                LettuceNearCacheConfig()
            ),
        ): ResilientLettuceSuspendNearCache<String> =
            ResilientLettuceSuspendNearCache(redisClient, lettuceDefaultCodec(), config)
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: LettuceLocalCache<String, V> = LettuceCaffeineLocalCache(config.base)
    private val connection: StatefulRedisConnection<String, V> = redisClient.connect(codec)
    private val commands: RedisCoroutinesCommands<String, V> = connection.coroutines()
    private val trackingListener: TrackingInvalidationListener<V> =
        TrackingInvalidationListener(frontCache, connection, config.cacheName)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<BackCacheCommand<String, V>>(capacity = config.writeQueueCapacity)

    private val retry: Retry = buildRetry()

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     * remove 후 write-behind 완료 전까지 get/containsKey가 Redis를 읽어 stale 값을 반환하는 것을 방지.
     * applyCommand에서 Redis 삭제 완료 후 tombstone 제거.
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 Redis read를 차단하는 플래그.
     * clearAll 이후 get이 Redis에서 stale 값을 읽는 것을 방지.
     */
    private val clearPending = atomic(false)

    init {
        if (config.useRespProtocol3) {
            runCatching { trackingListener.start() }
                .onFailure { e ->
                    log.warn(e) { "CLIENT TRACKING start failed, cache will work without invalidation: ${e.message}" }
                }
        }
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
                    withRetry<Unit>(retry) { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "Redis write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            }
        }
    }

    private suspend fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put    -> setRedis(cmd.key, cmd.value)
            is BackCacheCommand.PutAll -> cmd.entries.forEach { (k, v) -> setRedis(k, v) }
            is BackCacheCommand.Remove -> {
                commands.del(config.redisKey(cmd.key))
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                val rKeys = cmd.keys.map { config.redisKey(it) }.toTypedArray()
                if (rKeys.isNotEmpty()) commands.del(*rKeys)
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                clearBackCache()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redis GET → front populate → return
     * - Redis 실패 시 [GetFailureStrategy]에 따라 처리
     */
    suspend fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching { commands.get(config.redisKey(key)) }
                    .onFailure { e -> log.warn(e) { "Redis GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value -> frontCache.put(key, value) }

            GetFailureStrategy.PROPAGATE_EXCEPTION ->
                commands.get(config.redisKey(key))?.also { value ->
                    frontCache.put(key, value)
                }
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    suspend fun getAll(keys: Set<String>): Map<String, V> {
        if (clearPending.value) return emptyMap()
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = (keys - result.keys).filter { !tombstones.contains(it) }

        missedKeys.forEach { key ->
            val value = runCatching { commands.get(config.redisKey(key)) }
                .onFailure { e -> log.warn(e) { "Redis GET failed for key=$key during getAll" } }
                .getOrNull()
            if (value != null) {
                result[key] = value
                frontCache.put(key, value)
            }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - Redis write는 channel로 큐잉 (write-behind)
     */
    suspend fun put(key: String, value: V) {
        key.requireNotBlank("key")
        tombstones.remove(key)
        frontCache.put(key, value)
        writeChannel.trySend(BackCacheCommand.Put(key, value)).also { result ->
            if (result.isFailure) {
                log.warn { "Write channel full, dropping Put for key=$key" }
            }
        }
    }

    /**
     * 여러 키-값 쌍을 저장한다.
     */
    suspend fun putAll(entries: Map<String, V>) {
        tombstones.removeAll(entries.keys)
        entries.forEach { (k, v) -> frontCache.put(k, v) }
        writeChannel.trySend(BackCacheCommand.PutAll(entries)).also { result ->
            if (result.isFailure) {
                log.warn { "Write channel full, dropping PutAll for ${entries.size} entries" }
            }
        }
    }

    /**
     * 키-값이 없을 때만 저장한다. Redis setnx를 사용하여 원자적으로 처리한다.
     * @return 이미 존재하는 값이 있으면 기존 값, 새로 저장하면 null
     */
    suspend fun putIfAbsent(key: String, value: V): V? {
        val existing = get(key)
        if (existing != null) return existing

        val rKey = config.redisKey(key)
        val setted = commands.setnx(rKey, value) == true
        return if (setted) {
            config.redisTtl?.let { ttl -> commands.expire(rKey, ttl.seconds) }
            frontCache.put(key, value)
            null
        } else {
            commands.get(rKey)
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
            if ((commands.exists(config.redisKey(key)) ?: 0L) == 0L) return false
        }
        val ok = commands.set(config.redisKey(key), value, SetArgs.Builder.xx()) != null
        if (ok) {
            frontCache.put(key, value)
        }
        return ok
    }

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    suspend fun replace(key: String, oldValue: V, newValue: V): Boolean {
        val current = get(key) ?: return false
        if (current != oldValue) return false
        return replace(key, newValue)
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
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redis).
     */
    suspend fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return (commands.exists(config.redisKey(key)) ?: 0L) > 0L
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - Redis delete는 channel로 큐잉 (write-behind)
     */
    suspend fun remove(key: String) {
        key.requireNotBlank("key")
        frontCache.remove(key)
        tombstones.add(key)
        writeChannel.trySend(BackCacheCommand.Remove(key)).also { result ->
            if (result.isFailure) {
                tombstones.remove(key)
                log.warn { "Write channel full, dropping Remove for key=$key" }
            }
        }
    }

    /**
     * 여러 키에 해당하는 캐시 항목을 제거한다.
     */
    suspend fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        tombstones.addAll(keys)
        writeChannel.trySend(BackCacheCommand.RemoveAll(keys)).also { result ->
            if (result.isFailure) {
                tombstones.removeAll(keys)
                log.warn { "Write channel full, dropping RemoveAll for ${keys.size} keys" }
            }
        }
    }

    /**
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    fun clearFrontCache() {
        frontCache.clear()
        log.debug { "Front cache cleared for cacheName=${config.cacheName}" }
    }

    /**
     * 로컬 캐시와 Redis를 모두 비운다.
     * SCAN으로 이 cacheName의 key만 삭제한다 (다른 cacheName 데이터 보존).
     */
    suspend fun clearAll() {
        clearFrontCache()
        clearPending.value = true
        writeChannel.trySend(BackCacheCommand.ClearBack()).also { result ->
            if (result.isFailure) {
                clearPending.value = false
                log.warn { "Write channel full, dropping ClearBack for cacheName=${config.cacheName}" }
            }
        }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    suspend fun backCacheSize(): Long {
        val pattern = "${config.cacheName}:*"
        var count = 0L
        var cursor: ScanCursor = ScanCursor.INITIAL
        var finished = false
        while (!finished) {
            val result: KeyScanCursor<String>? =
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            if (result != null) {
                count += result.keys.size
                finished = result.isFinished
                cursor = result
            } else {
                finished = true
            }
        }
        return count
    }

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { scope.cancel() }
            runCatching { writeChannel.close() }
            runCatching { trackingListener.close() }
            runCatching { connection.close() }
            runCatching { frontCache.close() }
            log.debug { "ResilientLettuceSuspendNearCache [${config.cacheName}] closed" }
        }
    }

    private suspend fun clearBackCache() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        var finished = false
        while (!finished) {
            val result: KeyScanCursor<String>? =
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            if (result != null) {
                if (result.keys.isNotEmpty()) {
                    commands.del(*result.keys.toTypedArray())
                }
                finished = result.isFinished
                cursor = result
            } else {
                finished = true
            }
        }
        log.debug { "Redis back cache cleared for cacheName=${config.cacheName}" }
    }

    private val redisTtlArgs: SetArgs? by lazy {
        config.redisTtl?.let { SetArgs.Builder.ex(it) }
    }

    private suspend fun setRedis(key: String, value: V) {
        val rKey = config.redisKey(key)

        if (redisTtlArgs != null) {
            commands.set(rKey, value, redisTtlArgs!!)
        } else {
            commands.set(rKey, value)
        }
    }

    private val redisTtlExArgs: MSetExArgs? by lazy {
        config.redisTtl?.let { MSetExArgs.Builder.ex(it) }
    }

    private suspend fun msetRedis(map: Map<String, V>) {
        val rMap = map.map { config.redisKey(it.key) to it.value }.toMap()

        if (redisTtlExArgs != null) {
            commands.msetex(rMap, redisTtlExArgs!!)
        } else {
            commands.mset(rMap)
        }
    }
}
