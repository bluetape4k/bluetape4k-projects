package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec

/**
 * Redisson + Lettuce RESP3 하이브리드 Near Cache (2-tier cache) - Coroutine(Suspend) 구현.
 *
 * ## 아키텍처
 * ```
 * Application (suspend)
 *     |
 * [RedissonResp3SuspendNearCache]
 *     |
 * +--------+--------+----------+
 * |        |        |          |
 * Front   Back    Tracking
 * Caffeine Redisson  Lettuce RESP3
 * (local) (RBucket)  (push invalidation)
 * ```
 *
 * ## 동작 방식
 * - Read: front hit → return / front miss → Redisson getAsync → front populate + tracking 활성화 → return
 * - Write: front put + Redisson setAsync (write-through) + Lettuce coroutines GET (tracking 활성화)
 * - Invalidation: RESP3 CLIENT TRACKING push → [CaffeineRedissonLocalCache.invalidate]
 *
 * ## NOLOOP 동작 주의사항
 * Redisson 데이터 연결과 Lettuce tracking 연결은 별도 연결이므로,
 * 자신이 Redisson으로 쓴 키도 Lettuce tracking 연결에 invalidation이 전파될 수 있다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedissonResp3SuspendNearCache<V: Any>(
    private val redisson: RedissonClient,
    private val redisClient: RedisClient,
    private val redissonCodec: Codec = RedissonNearCache.defaultNearCacheCodec,
    private val config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
): AutoCloseable {

    companion object: KLogging() {
        /**
         * String 키/값 타입의 Near Suspend Cache를 생성한다.
         */
        operator fun invoke(
            redisson: RedissonClient,
            redisClient: RedisClient,
            config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
        ): RedissonResp3SuspendNearCache<String> =
            RedissonResp3SuspendNearCache(redisson, redisClient, RedissonNearCache.defaultNearCacheCodec, config)
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: RedissonLocalCache<String, V> = CaffeineRedissonLocalCache(config)

    // Lettuce tracking 전용 연결 (StringCodec — key 이름만 필요)
    private val trackingConnection: StatefulRedisConnection<String, String> =
        redisClient.connect(StringCodec.UTF8)
    private val trackingCommands: RedisCoroutinesCommands<String, String> =
        trackingConnection.coroutines()

    private val trackingListener: RedissonTrackingInvalidationListener<V> =
        RedissonTrackingInvalidationListener(frontCache, trackingConnection, config.cacheName, redisClient)

    init {
        if (config.useRespProtocol3) {
            runCatching { trackingListener.start() }
                .onFailure { e ->
                    log.warn(e) { "CLIENT TRACKING start failed, cache will work without invalidation: ${e.message}" }
                }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redisson getAsync → front populate + tracking 활성화 → return
     */
    suspend fun get(key: String): V? {
        key.requireNotBlank("key")

        frontCache.get(key)?.let { return it }

        @Suppress("UNCHECKED_CAST")
        return redisson.getBucket<V>(config.redisKey(key), redissonCodec)
            .getAsync().await()
            ?.also { value ->
                frontCache.put(key, value)
                // CLIENT TRACKING 활성화
                trackingCommands.get(config.redisKey(key))
            }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     *
     * front cache miss된 키들을 [coroutineScope] 내에서 병렬 [async]로 조회하므로
     * 순차 await 대비 전체 응답 시간을 단축한다.
     */
    suspend fun getAll(keys: Set<String>): Map<String, V> {
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = keys - result.keys

        if (missedKeys.isEmpty()) return result

        coroutineScope {
            missedKeys.map { key ->
                async {
                    @Suppress("UNCHECKED_CAST")
                    redisson.getBucket<V>(config.redisKey(key), redissonCodec)
                        .getAsync().await()
                        ?.let { value -> key to value }
                }
            }.awaitAll().filterNotNull().forEach { (key, value) ->
                result[key] = value
                frontCache.put(key, value)
                trackingCommands.get(config.redisKey(key))
            }
        }

        return result
    }

    /**
     * key-value를 저장한다 (write-through).
     * front cache + Redisson setAsync (TTL 있으면 setAsync with TTL).
     *
     * write-through 후 Lettuce coroutines GET을 실행해 CLIENT TRACKING을 활성화한다.
     */
    suspend fun put(key: String, value: V) {
        key.requireNotBlank("key")

        frontCache.put(key, value)
        setRedis(key, value)
        trackingCommands.get(config.redisKey(key))
    }

    /**
     * 여러 key-value를 한 번에 저장한다.
     *
     * Redis 쓰기([setRedis])와 tracking 활성화([trackingCommands.get])를 각 키별로
     * 하나의 [async] 블록에서 처리하여 순차 쓰기 대비 전체 응답 시간을 단축한다.
     */
    suspend fun putAll(map: Map<String, V>) {
        frontCache.putAll(map)
        coroutineScope {
            map.map { (key, value) ->
                async {
                    setRedis(key, value)
                    trackingCommands.get(config.redisKey(key))
                }
            }.awaitAll()
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
     * 키를 제거한다 (front + Redisson).
     */
    suspend fun remove(key: String) {
        key.requireNotBlank("key")

        frontCache.remove(key)
        redisson.getBucket<V>(config.redisKey(key), redissonCodec).deleteAsync().await()
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    suspend fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        val rkeys = keys.map { config.redisKey(it) }.toTypedArray()
        if (rkeys.isNotEmpty()) {
            redisson.keys.deleteAsync(*rkeys).await()
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다.
     * 키가 존재하는 경우에만 교체한다.
     * @return 교체 성공 여부
     */
    suspend fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")

        @Suppress("UNCHECKED_CAST")
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        if (bucket.isExistsAsync().await() != true) return false
        setRedis(key, value)
        frontCache.put(key, value)
        trackingCommands.get(config.redisKey(key))
        return true
    }

    /**
     * 기존 값이 oldValue와 같을 때만 newValue로 교체한다.
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
        if (value != null) {
            remove(key)
        }
        return value
    }

    /**
     * 조회 후 교체한다.
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
        if (frontCache.containsKey(key)) return true
        return redisson.getBucket<V>(config.redisKey(key), redissonCodec).isExistsAsync().await() == true
    }

    /**
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    fun clearLocal() {
        frontCache.clear()
    }

    /**
     * 로컬 캐시 + Redis를 모두 비운다.
     * SCAN으로 이 cacheName의 key만 삭제한다 (다른 cacheName의 데이터 보존).
     */
    suspend fun clearAll() {
        clearLocal()
        clearBack()
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localSize(): Long = frontCache.estimatedSize()

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
     * 로컬 캐시(Caffeine) 통계. [RedissonResp3NearCacheConfig.recordStats]가 true일 때만 유효.
     */
    fun localStats(): CacheStats? = frontCache.stats()

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { trackingListener.close() }
            runCatching { trackingConnection.close() }
            runCatching { frontCache.close() }
            log.debug { "RedissonResp3SuspendNearCache [${config.cacheName}] closed" }
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
