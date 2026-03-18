package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.cache.lettuceDefaultCodec
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.MSetExArgs
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.RedisCodec
import kotlinx.atomicfu.atomic

/**
 * Lettuce 기반 Near Cache (2-tier cache) - 동기(Blocking) 구현.
 *
 * ## 아키텍처
 * ```
 * Application
 *     |
 * [LettuceNearCache]
 *     |
 * +---+---+
 * |       |
 * Front   Back
 * Caffeine  Redis (via Lettuce)
 *
 * Invalidation: Redis CLIENT TRACKING (RESP3) -> server push -> local invalidate
 * ```
 *
 * ## Key 격리 전략
 * Redis key는 `{cacheName}:{key}` 형태의 prefix를 사용한다.
 * - cacheName별 독립적인 key 공간 보장
 * - `clearAll()`은 SCAN으로 해당 cacheName의 key만 삭제 (FLUSHDB 금지)
 * - CLIENT TRACKING은 key 단위로 동작하여 정확한 invalidation 보장
 *
 * - Read: front hit → return / front miss → Redis GET → front populate → return
 * - Write: front put + Redis SET (write-through)
 * - Invalidation: RESP3 CLIENT TRACKING push → [LettuceCaffeineLocalCache.invalidate]
 *
 * @param V 값 타입 (키는 항상 String)
 */
class LettuceNearCache<V: Any>(
    redisClient: RedisClient,
    codec: RedisCodec<String, V> = lettuceDefaultCodec(),
    private val config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
): LettuceNearCacheOperations<V> {
    companion object: KLogging() {
        private const val COMPARE_AND_SET_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            if current == false or current ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'XX', 'KEEPTTL')
            return 1
        """

        /**
         * String 키/값 타입의 Near Cache를 생성한다.
         */
        operator fun invoke(
            redisClient: RedisClient,
            config: LettuceNearCacheConfig<String, String> = LettuceNearCacheConfig(),
        ): LettuceNearCache<String> = LettuceNearCache(redisClient, lettuceDefaultCodec(), config)
    }

    override val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    override val isClosed by closed
    private val setArgsPx: SetArgs? = config.redisTtl?.let { SetArgs.Builder.px(it) }
    private val setArgsNx: SetArgs = SetArgs.Builder.nx()
    private val setArgsNxPx: SetArgs? = config.redisTtl?.let { SetArgs.Builder.nx().px(it) }
    private val setArgsXxKeepTtl: SetArgs = SetArgs.Builder.xx().keepttl()
    private val msetExArgs: MSetExArgs? = config.redisTtl?.let { MSetExArgs.Builder.ex(it) }

    private val frontCache: LettuceLocalCache<String, V> = LettuceCaffeineLocalCache(config)
    private val connection: StatefulRedisConnection<String, V> = redisClient.connect(codec)
    private val commands: RedisCommands<String, V> = connection.sync()
    private val trackingListener: TrackingInvalidationListener<V> =
        TrackingInvalidationListener(frontCache, connection, config.cacheName)

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
     * - front miss → Redis GET → front populate → return
     */
    override fun get(key: String): V? {
        key.requireNotBlank("key")

        frontCache.get(key)?.let { return it }

        return commands.get(config.redisKey(key))?.also { value ->
            frontCache.put(key, value)
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다 (multi-get).
     */
    override fun getAll(keys: Set<String>): Map<String, V> {
        keys.requireNotEmpty("keys")

        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = keys - result.keys.toSet()

        if (missedKeys.isNotEmpty()) {
            val pipeline: RedisAsyncCommands<String, V> = connection.async()
            val futures: Map<String, RedisFuture<V>> =
                missedKeys.associateWith { key ->
                    pipeline.get(config.redisKey(key))
                }
            connection.flushCommands()
            futures.forEach { (key, future) ->
                future.get()?.let { value ->
                    result[key] = value
                    frontCache.put(key, value)
                }
            }
        }

        return result
    }

    /**
     * key-value를 저장한다 (write-through).
     * front cache + Redis SET (TTL 있으면 SETEX).
     *
     * write-through 후 async Redis GET을 fire-and-forget으로 실행해 CLIENT TRACKING을 활성화한다.
     */
    override fun put(
        key: String,
        value: V,
    ) {
        key.requireNotBlank("key")
        setRedis(key, value)
        frontCache.put(key, value)
        registerTrackingKey(key)
    }

    /**
     * 여러 key-value를 한 번에 저장한다.
     */
    override fun putAll(map: Map<out String, V>) {
        if (map.isEmpty()) return

        val normalizedMap =
            map.entries.associate { (key, value) ->
                key.requireNotBlank("key") to value
            }
        setRedisBulk(normalizedMap)
        frontCache.putAll(normalizedMap)
        registerTrackingKeys(normalizedMap.keys)
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    override fun putIfAbsent(
        key: String,
        value: V,
    ): V? {
        val existing = get(key)
        if (existing != null) return existing

        val rKey = config.redisKey(key)
        val status = setNxRedis(key, value)
        val setted = status == "OK"
        return if (setted) {
            frontCache.put(key, value)
            registerTrackingKey(key)
            null
        } else {
            commands.get(rKey)
        }
    }

    /**
     * 키를 제거한다 (front + Redis).
     */
    override fun remove(key: String) {
        frontCache.remove(key)
        commands.del(config.redisKey(key))
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    override fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        val rkeys = keys.map { config.redisKey(it) }
        commands.del(*rkeys.toTypedArray())
    }

    /**
     * 기존 값을 새 값으로 교체한다.
     * @return 교체 성공 여부
     */
    override fun replace(
        key: String,
        value: V,
    ): Boolean {
        commands.get(config.redisKey(key)) ?: return false
        val ok = commands.set(config.redisKey(key), value, SetArgs.Builder.xx()) != null
        if (ok) {
            frontCache.put(key, value)
            registerTrackingKey(key)
        }
        return ok
    }

    /**
     * 기존 값이 oldValue와 같을 때만 newValue로 교체한다.
     */
    override fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean {
        val replaced =
            commands.eval<Long>(
                COMPARE_AND_SET_SCRIPT,
                ScriptOutputType.INTEGER,
                arrayOf(config.redisKey(key)),
                oldValue,
                newValue
            ) == 1L
        if (replaced) {
            frontCache.put(key, newValue)
            registerTrackingKey(key)
        }
        return replaced
    }

    /**
     * 조회 후 제거한다.
     */
    override fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) {
            remove(key)
        }
        return value
    }

    /**
     * 조회 후 교체한다.
     */
    override fun getAndReplace(
        key: String,
        value: V,
    ): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redis).
     */
    override fun containsKey(key: String): Boolean {
        if (frontCache.containsKey(key)) return true
        return commands.exists(config.redisKey(key)) > 0
    }

    /**
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    override fun clearLocal() {
        frontCache.clear()
    }

    private fun clearBack() {
        val pattern = "${config.cacheName}:*"
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(NearCache.SCAN_BATCH_SIZE))
            if (result.keys.isNotEmpty()) {
                commands.del(*result.keys.toTypedArray())
            }
            cursor = result
        } while (!result.isFinished)
    }

    /**
     * 로컬 캐시 + Redis를 모두 비운다.
     * SCAN으로 이 cacheName의 key만 삭제한다 (다른 cacheName의 데이터 보존).
     */
    override fun clearAll() {
        clearLocal()
        runCatching { clearBack() }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    override fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    override fun backCacheSize(): Long {
        val pattern = "${config.cacheName}:*"
        var count = 0L
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val result: KeyScanCursor<String> =
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(NearCache.SCAN_BATCH_SIZE))
            count += result.keys.size
            cursor = result
        } while (!result.isFinished)
        return count
    }

    /**
     * 로컬 캐시(Caffeine) 통계. [LettuceNearCacheConfig.recordStats]가 true일 때만 유효한 값을 반환한다.
     */
    override fun localStats(): CacheStats? = frontCache.stats()

    /**
     * 모든 리소스를 정리하고 연결을 닫는다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { trackingListener.close() }
            runCatching { connection.close() }
            runCatching { frontCache.close() }
            log.debug { "LettuceNearCache [${config.cacheName}] closed" }
        }
    }

    private fun setRedis(
        key: String,
        value: V,
    ): String? {
        val rKey = config.redisKey(key)
        return if (setArgsPx != null) {
            commands.set(rKey, value, setArgsPx)
        } else {
            commands.set(rKey, value)
        }
    }

    private fun setNxRedis(
        key: String,
        value: V,
    ): String? {
        val rKey = config.redisKey(key)
        return if (setArgsNxPx != null) {
            commands.set(rKey, value, setArgsNxPx)
        } else {
            commands.set(rKey, value, setArgsNx)
        }
    }

    private fun setRedisBulk(map: Map<String, V>) {
        val redisMap = map.entries.associate { (key, value) -> config.redisKey(key) to value }
        if (msetExArgs != null) {
            val applied = commands.msetex(redisMap, msetExArgs)
            check(applied == true) { "Redis MSETEX failed for cacheName=${config.cacheName}" }
        } else {
            val status = commands.mset(redisMap)
            check(status == "OK") { "Redis MSET failed for cacheName=${config.cacheName}: $status" }
        }
    }

    private fun registerTrackingKey(key: String) {
        // CLIENT TRACKING 활성화: 다른 인스턴스가 이 키를 수정할 때 invalidation을 받을 수 있도록
        connection.async().get(config.redisKey(key))
    }

    private fun registerTrackingKeys(keys: Collection<String>) {
        if (keys.isEmpty()) return
        connection.async().mget(*keys.map(config::redisKey).toTypedArray())
    }
}
