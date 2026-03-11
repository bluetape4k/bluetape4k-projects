package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.atomicfu.atomic
import org.redisson.api.BatchOptions
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec


/**
 * Redisson + Lettuce RESP3 하이브리드 Near Cache (2-tier cache) - 동기(Blocking) 구현.
 *
 * ## 아키텍처
 * ```
 * Application
 *     |
 * [RedissonResp3NearCache]
 *     |
 * +--------+--------+----------+
 * |        |        |          |
 * Front   Back    Tracking
 * Caffeine Redisson  Lettuce RESP3
 * (local) (RBucket)  (push invalidation)
 * ```
 *
 * ## 동작 방식
 * - Read: front hit → return / front miss → Redisson GET → front populate → return
 * - Write: front put + Redisson SET (write-through) + Lettuce GET (tracking 활성화)
 * - Invalidation: RESP3 CLIENT TRACKING push → [CaffeineRedissonLocalCache.invalidate]
 *
 * ## NOLOOP 동작 주의사항
 * Redisson 데이터 연결과 Lettuce tracking 연결은 별도 연결이므로,
 * 자신이 Redisson으로 쓴 키도 Lettuce tracking 연결에 invalidation이 전파될 수 있다.
 * 이는 cache-lettuce의 단일 연결 방식과 다른 동작이다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
class RedissonResp3NearCache<V: Any>(
    private val redisson: RedissonClient,
    private val redisClient: RedisClient,
    private val redissonCodec: Codec = RedissonNearCache.defaultNearCacheCodec,
    private val config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
): AutoCloseable {

    companion object: KLogging() {
        /**
         * String 키/값 타입의 Near Cache를 생성한다.
         */
        operator fun invoke(
            redisson: RedissonClient,
            redisClient: RedisClient,
            config: RedissonResp3NearCacheConfig = RedissonResp3NearCacheConfig(),
        ): RedissonResp3NearCache<String> =
            RedissonResp3NearCache(redisson, redisClient, RedissonNearCache.defaultNearCacheCodec, config)
    }

    val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    val isClosed by closed

    private val frontCache: RedissonLocalCache<String, V> = CaffeineRedissonLocalCache(config)

    // Lettuce tracking 전용 연결 (StringCodec — key 이름만 필요, 값 타입 무관)
    private val trackingConnection: StatefulRedisConnection<String, String> =
        redisClient.connect(StringCodec.UTF8)
    private val trackingSync: RedisCommands<String, String> = trackingConnection.sync()

    private val trackingListener: RedissonTrackingInvalidationListener<V> =
        RedissonTrackingInvalidationListener(frontCache, trackingConnection, config.cacheName, redisClient)

    init {
        if (config.useRespProtocol3) {
            runCatching { trackingListener.start() }
                .onFailure { e ->
                    log.warn(e) { "CLIENT TRACKING start failed, cache will work without invalidation" }
                }
        }
    }

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redisson GET → front populate + tracking 활성화 → return
     */
    fun get(key: String): V? {
        key.requireNotBlank("key")

        frontCache.get(key)?.let { return it }

        @Suppress("UNCHECKED_CAST")
        return redisson.getBucket<V>(config.redisKey(key), redissonCodec).get()?.also { value ->
            frontCache.put(key, value)
            // CLIENT TRACKING 활성화: sync 호출로 tracking이 실제로 등록된 후 반환 (경쟁 조건 방지)
            trackingSync.get(config.redisKey(key))
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     *
     * Redisson Batch API를 사용해 missedKeys를 병렬로 조회하므로,
     * 순차 조회 대비 네트워크 왕복(RTT)을 1회로 줄인다.
     */
    fun getAll(keys: Set<String>): Map<String, V> {
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = keys - result.keys

        if (missedKeys.isEmpty()) return result

        val batch = redisson.createBatch(BatchOptions.defaults())
        val futures = missedKeys.map { key ->
            @Suppress("UNCHECKED_CAST")
            key to batch.getBucket<V>(config.redisKey(key), redissonCodec).getAsync()
        }
        batch.execute()

        // batch.execute() 완료 이후에는 future가 이미 완료 상태이므로 get()은 즉시 반환
        futures.forEach { (key, future) ->
            @Suppress("UNCHECKED_CAST")
            (future.get() as? V)?.let { value ->
                result[key] = value
                frontCache.put(key, value)
                trackingSync.get(config.redisKey(key))
            }
        }

        return result
    }

    /**
     * key-value를 저장한다 (write-through).
     * front cache + Redisson SET (TTL 있으면 SETEX).
     *
     * write-through 후 async Lettuce GET을 fire-and-forget으로 실행해 CLIENT TRACKING을 활성화한다.
     */
    fun put(key: String, value: V) {
        key.requireNotBlank("key")

        frontCache.put(key, value)
        setRedis(key, value)
        // CLIENT TRACKING 활성화
        trackingConnection.async().get(config.redisKey(key))
    }

    /**
     * 여러 key-value를 한 번에 저장한다.
     *
     * Redisson Batch API를 사용해 모든 SET 명령을 한 번의 배치로 실행하므로
     * 순차 쓰기 대비 네트워크 왕복(RTT)을 1회로 줄인다.
     */
    fun putAll(map: Map<out String, V>) {
        frontCache.putAll(map)

        val batch = redisson.createBatch(BatchOptions.defaults())
        map.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            val bucket = batch.getBucket<V>(config.redisKey(key), redissonCodec)
            val ttl = config.redisTtl
            if (ttl != null) {
                bucket.setAsync(value, ttl)
            } else {
                bucket.setAsync(value)
            }
        }
        batch.execute()

        map.keys.forEach { key ->
            trackingConnection.async().get(config.redisKey(key))
                .exceptionally { e ->
                    log.warn(e) { "CLIENT TRACKING 활성화 실패. key=$key, cacheName=${config.cacheName}" }
                    null
                }
        }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: String, value: V): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        @Suppress("UNCHECKED_CAST")
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val setted = if (config.redisTtl != null) {
            bucket.setIfAbsent(value, config.redisTtl)
        } else {
            bucket.setIfAbsent(value)
        }

        return if (setted) {
            frontCache.put(key, value)
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            bucket.get()
        }
    }

    /**
     * 키를 제거한다 (front + Redisson).
     */
    fun remove(key: String) {
        key.requireNotBlank("key")

        frontCache.remove(key)
        redisson.getBucket<V>(config.redisKey(key), redissonCodec).delete()
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        val rkeys = keys.map { config.redisKey(it) }.toTypedArray()
        if (rkeys.isNotEmpty()) {
            redisson.keys.delete(*rkeys)
        }
    }

    /**
     * 기존 값을 새 값으로 교체한다.
     * 키가 존재하는 경우에만 교체한다.
     * @return 교체 성공 여부
     */
    fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")

        @Suppress("UNCHECKED_CAST")
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        if (!bucket.isExists) return false
        setRedis(key, value)
        frontCache.put(key, value)
        trackingConnection.async().get(config.redisKey(key))
        return true
    }

    /**
     * 기존 값이 oldValue와 같을 때만 newValue로 교체한다.
     */
    fun replace(key: String, oldValue: V, newValue: V): Boolean {
        val current = get(key) ?: return false
        if (current != oldValue) return false
        return replace(key, newValue)
    }

    /**
     * 조회 후 제거한다.
     */
    fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) {
            remove(key)
        }
        return value
    }

    /**
     * 조회 후 교체한다.
     */
    fun getAndReplace(key: String, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redisson).
     */
    fun containsKey(key: String): Boolean {
        if (frontCache.containsKey(key)) return true
        return redisson.getBucket<V>(config.redisKey(key), redissonCodec).isExists
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
    fun clearAll() {
        clearLocal()
        runCatching { clearBack() }
            .onFailure { e -> log.warn(e) { "Redis 백 캐시 삭제 중 오류 발생. cacheName=${config.cacheName}" } }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    fun backCacheSize(): Long {
        val pattern = "${config.cacheName}:*"
        var count = 0L
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                trackingSync.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            count += result.keys.size
            cursor = result
        } while (!result.isFinished)
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
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { trackingListener.close() }
            runCatching { trackingConnection.close() }
            runCatching { frontCache.close() }
            log.debug { "RedissonResp3NearCache [${config.cacheName}] closed" }
        }
    }

    private fun clearBack() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                trackingSync.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100))
            if (result.keys.isNotEmpty()) {
                trackingSync.del(*result.keys.toTypedArray())
            }
            cursor = result
        } while (!result.isFinished)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setRedis(key: String, value: V) {
        val bucket = redisson.getBucket<V>(config.redisKey(key), redissonCodec)
        val ttl = config.redisTtl
        if (ttl != null) {
            bucket.set(value, ttl)
        } else {
            bucket.set(value)
        }
    }
}
