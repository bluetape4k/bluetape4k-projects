package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import java.util.concurrent.ConcurrentHashMap

/**
 * Resilient Redisson + Lettuce RESP3 하이브리드 Near Cache (2-tier: Caffeine front + Redisson back) - Coroutine(Suspend) 구현.
 *
 * [RedissonResp3SuspendNearCache]와 동일한 public API를 유지하면서,
 * Redisson back cache 쓰기에 대해 다음을 추가한다:
 *
 * - **write-behind**: put/remove는 front cache에 즉시 반영하고,
 *   Redisson 쓰기는 [Channel]에 큐잉하여 consumer coroutine이 순차 처리
 * - **retry**: consumer에서 resilience4j [Retry]로 재시도 (지수 백오프 옵션)
 * - **get graceful degradation**: Redisson GET 실패 시 front 값 반환 또는 null
 * - **invalidation**: Lettuce RESP3 CLIENT TRACKING push로 front cache 무효화
 *
 * ```
 * Application (suspend)
 *     |
 * [ResilientRedissonResp3SuspendNearCache]
 *     |
 * +---+--------+
 * |            |
 * Front        Write Channel (Channel<BackCacheCommand>)
 * Caffeine        |
 * (즉시반영)   Consumer Coroutine
 *              (withRetry + redisson.getBucket().setAsync/deleteAsync.await())
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class ResilientRedissonResp3SuspendNearCache<V: Any>(
    private val redisson: RedissonClient,
    private val redisClient: RedisClient,
    private val redissonCodec: Codec = RedissonNearCache.defaultNearCacheCodec,
    private val config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(
        RedissonResp3NearCacheConfig()
    ),
): AutoCloseable {

    companion object: KLogging() {
        /**
         * String 키/값 타입의 Resilient Redisson RESP3 Suspend Near Cache를 생성한다.
         */
        operator fun invoke(
            redisson: RedissonClient,
            redisClient: RedisClient,
            config: ResilientRedissonResp3NearCacheConfig = ResilientRedissonResp3NearCacheConfig(
                RedissonResp3NearCacheConfig()
            ),
        ): ResilientRedissonResp3SuspendNearCache<String> =
            ResilientRedissonResp3SuspendNearCache(
                redisson,
                redisClient,
                RedissonNearCache.defaultNearCacheCodec,
                config
            )
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: RedissonLocalCache<String, V> = CaffeineRedissonLocalCache(config.base)

    // Lettuce tracking 전용 연결 (StringCodec — key 이름만 필요)
    private val trackingConnection: StatefulRedisConnection<String, String> =
        redisClient.connect(StringCodec.UTF8)
    private val trackingCommands: RedisCoroutinesCommands<String, String> =
        trackingConnection.coroutines()

    private val trackingListener: RedissonTrackingInvalidationListener<V> =
        RedissonTrackingInvalidationListener(frontCache, trackingConnection, config.cacheName)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<BackCacheCommand<String, V>>(capacity = config.writeQueueCapacity)

    private val retry: Retry = buildRetry()

    /**
     * write-behind로 삭제 요청된 키 집합 (tombstone).
     */
    private val tombstones: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * clearAll 호출 후 ClearBack이 처리될 때까지 Redis read를 차단하는 플래그.
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
                    retry.executeSuspendFunction { applyCommand(cmd) }
                } catch (e: Exception) {
                    log.error(e) { "Redisson write failed after ${config.retryMaxAttempts} retries, dropping command: $cmd" }
                }
            }
        }
    }

    private suspend fun applyCommand(cmd: BackCacheCommand<String, V>) {
        when (cmd) {
            is BackCacheCommand.Put    -> {
                setRedis(cmd.key, cmd.value)
                trackingCommands.get(config.redisKey(cmd.key))
            }
            is BackCacheCommand.PutAll -> {
                cmd.entries.forEach { (k, v) ->
                    setRedis(k, v)
                }
                cmd.entries.keys.forEach { k ->
                    trackingCommands.get(config.redisKey(k))
                }
            }
            is BackCacheCommand.Remove -> {
                redisson.getBucket<V>(config.redisKey(cmd.key), redissonCodec).deleteAsync().await()
                tombstones.remove(cmd.key)
            }
            is BackCacheCommand.RemoveAll -> {
                val rkeys = cmd.keys.map { config.redisKey(it) }.toTypedArray()
                if (rkeys.isNotEmpty()) redisson.keys.deleteAsync(*rkeys).await()
                tombstones.removeAll(cmd.keys)
            }
            is BackCacheCommand.ClearBack -> {
                clearBack()
                tombstones.clear()
                clearPending.value = false
            }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redisson getAsync → front populate + tracking 활성화 → return
     * - Redisson 실패 시 [GetFailureStrategy]에 따라 처리
     */
    suspend fun get(key: String): V? {
        key.requireNotBlank("key")

        if (tombstones.contains(key) || clearPending.value) return null
        frontCache.get(key)?.let { return it }

        return when (config.getFailureStrategy) {
            GetFailureStrategy.RETURN_FRONT_OR_NULL ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    redisson.getBucket<V>(config.redisKey(key), redissonCodec).getAsync().await()
                }
                    .onFailure { e -> log.warn(e) { "Redisson GET failed for key=$key, returning null" } }
                    .getOrNull()
                    ?.also { value ->
                        frontCache.put(key, value)
                        trackingCommands.get(config.redisKey(key))
                    }

            GetFailureStrategy.PROPAGATE_EXCEPTION -> {
                @Suppress("UNCHECKED_CAST")
                redisson.getBucket<V>(config.redisKey(key), redissonCodec).getAsync().await()
                    ?.also { value ->
                        frontCache.put(key, value)
                        trackingCommands.get(config.redisKey(key))
                    }
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
            runCatching {
                @Suppress("UNCHECKED_CAST")
                redisson.getBucket<V>(config.redisKey(key), redissonCodec).getAsync().await()
            }
                .onFailure { e -> log.warn(e) { "Redisson GET failed for key=$key during getAll" } }
                .getOrNull()
                ?.let { value ->
                    result[key] = value
                    frontCache.put(key, value)
                    trackingCommands.get(config.redisKey(key))
                }
        }
        return result
    }

    /**
     * 키-값 쌍을 저장한다.
     * - front cache 즉시 반영
     * - Redisson write는 channel로 큐잉 (write-behind)
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

        @Suppress("UNCHECKED_CAST")
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val setted = if (config.redisTtl != null) {
            bucket.setIfAbsentAsync(value, config.redisTtl).await()
        } else {
            bucket.setIfAbsentAsync(value).await()
        }
        return if (setted == true) {
            frontCache.put(key, value)
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            bucket.getAsync().await()
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다 (키가 있을 때만).
     * @return 교체 성공 여부
     */
    suspend fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")
        if (tombstones.contains(key) || clearPending.value) return false

        // front cache에 있으면 write-behind 중이더라도 교체 허용 (back 반영 전)
        if (!frontCache.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
            if (bucket.isExistsAsync().await() != true) return false
        }
        setRedis(key, value)
        frontCache.put(key, value)
        trackingCommands.get(config.redisKey(key))
        return true
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
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redisson).
     */
    suspend fun containsKey(key: String): Boolean {
        if (tombstones.contains(key) || clearPending.value) return false
        if (frontCache.containsKey(key)) return true
        return runCatching {
            redisson.getBucket<V>(config.redisKey(key), redissonCodec).isExistsAsync().await() == true
        }
            .onFailure { e -> log.warn(e) { "Redisson isExists failed for key=$key" } }
            .getOrDefault(false)
    }

    /**
     * 키에 해당하는 캐시 항목을 제거한다.
     * - front cache 즉시 반영
     * - Redisson delete는 channel로 큐잉 (write-behind)
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
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    fun clearLocal() {
        frontCache.clear()
        log.debug { "Front cache cleared for cacheName=${config.cacheName}" }
    }

    /**
     * 로컬 캐시와 Redis를 모두 비운다 (write-behind).
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
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    suspend fun backCacheSize(): Long {
        val pattern = "${config.cacheName}:*"
        var count = 0L
        var cursor: ScanCursor = ScanCursor.INITIAL
        var finished = false
        while (!finished) {
            val result: KeyScanCursor<String>? =
                trackingCommands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
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
     * 로컬 캐시(Caffeine) 통계.
     */
    fun localStats(): CacheStats? = frontCache.stats()

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { scope.cancel() }
            runCatching { writeChannel.close() }
            runCatching { trackingListener.close() }
            runCatching { trackingConnection.close() }
            runCatching { frontCache.close() }
            log.debug { "ResilientRedissonResp3SuspendNearCache [${config.cacheName}] closed" }
        }
    }

    private suspend fun clearBack() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        var finished = false
        while (!finished) {
            val result: KeyScanCursor<String>? =
                trackingCommands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            if (result != null) {
                if (result.keys.isNotEmpty()) {
                    trackingCommands.del(*result.keys.toTypedArray())
                }
                finished = result.isFinished
                cursor = result
            } else {
                finished = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun setRedis(key: String, value: V) {
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val ttl = config.redisTtl
        if (ttl != null) {
            bucket.setAsync(value, ttl).await()
        } else {
            bucket.setAsync(value).await()
        }
    }
}
