package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyScanCursor
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.sync.multi
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
    codec: RedisCodec<String, V> = LettuceBinaryCodecs.default(),
    private val config: LettuceNearCacheConfig<String, V> = LettuceNearCacheConfig(),
): NearCacheOperations<V> {
    companion object: KLogging() {
        // 개선: 기존엔 raw Lua 원문을 `commands.eval` 로 매번 전송해 네트워크/파싱 비용이
        //       반복해서 발생했습니다. 이제 [NearCacheScripts.COMPARE_AND_SET] 로 SHA1 을
        //       1회 계산해 두고 `evalsha` → NOSCRIPT 발생 시 원문 전송으로 fallback 합니다.
        private val COMPARE_AND_SET_SCRIPT = NearCacheScripts.COMPARE_AND_SET

        /**
         * String 키/값 타입의 Near Cache를 생성한다.
         */
        operator fun invoke(
            redisClient: RedisClient,
            config: LettuceNearCacheConfig<String, String> = LettuceNearCacheConfig(),
        ): LettuceNearCache<String> = LettuceNearCache(redisClient, LettuceBinaryCodecs.default(), config)
    }

    override val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    override val isClosed by closed

    private val backHitCount = atomic(0L)
    private val backMissCount = atomic(0L)

    private val setArgsPx: SetArgs? = config.redisTtl?.let { SetArgs.Builder.px(it) }
    private val setArgsNx: SetArgs = SetArgs.Builder.nx()
    private val setArgsNxPx: SetArgs? = config.redisTtl?.let { SetArgs.Builder.nx().px(it) }

    private val frontCache: LettuceLocalCache<String, V> = LettuceCaffeineLocalCache(config)
    private val connection: StatefulRedisConnection<String, V> = redisClient.connect(codec)
    private val commands: RedisCommands<String, V> = connection.sync()

    // 개선: `connection.async()` 는 매 호출마다 wrapper 인스턴스를 반환할 수 있어
    //       핫 패스 (`registerTrackingKey`, `registerTrackingKeys`, `getAll`) 에서 getter 를
    //       반복 호출하지 않도록 한 번 획득해 재사용한다.
    private val asyncCommands: RedisAsyncCommands<String, V> = connection.async()

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

        val backValue = commands.get(config.redisKey(key))
        return if (backValue != null) {
            backHitCount.incrementAndGet()
            frontCache.put(key, backValue)
            backValue
        } else {
            backMissCount.incrementAndGet()
            null
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
            // 개선: 재사용 가능한 `asyncCommands` 필드를 사용해 매 호출마다 생성되는
            //       wrapper 비용을 제거한다.
            val futures: Map<String, RedisFuture<V>> =
                missedKeys.associateWith { key ->
                    asyncCommands.get(config.redisKey(key))
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
    override fun putAll(entries: Map<String, V>) {
        if (entries.isEmpty()) return

        val normalizedMap =
            entries.entries.associate { (key, value) ->
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
        // 개선: `DEL` 은 큰 값 삭제 시 Redis 메인 스레드를 블로킹합니다. `UNLINK` 로 바꿔
        //       Redis 가 백그라운드에서 메모리를 해제하도록 해 p99 latency 를 낮춥니다.
        commands.unlink(config.redisKey(key))
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    override fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        val rkeys = keys.map { config.redisKey(it) }
        // 개선: 대량 삭제에서 `UNLINK` 의 이점이 크므로 `DEL` → `UNLINK` 로 변경합니다.
        commands.unlink(*rkeys.toTypedArray())
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
        // 개선: EVALSHA 우선 → NOSCRIPT 발생 시 원문 전송으로 fallback.
        //       SHA1 은 생성 시점에 한 번만 계산된 값을 재사용하므로 네트워크 페이로드가 20B 로 축소됩니다.
        val rKey = config.redisKey(key)
        val result: Long = try {
            commands.evalsha<Long>(
                COMPARE_AND_SET_SCRIPT.sha1,
                ScriptOutputType.INTEGER,
                arrayOf(rKey),
                oldValue,
                newValue
            )
        } catch (_: RedisNoScriptException) {
            commands.eval<Long>(
                COMPARE_AND_SET_SCRIPT.source,
                ScriptOutputType.INTEGER,
                arrayOf(rKey),
                oldValue,
                newValue
            )
        }
        val replaced = result == 1L
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
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100L))
            if (result.keys.isNotEmpty()) {
                // 개선: 대량 키 일괄 삭제에 `UNLINK` 를 사용해 Redis 메인 스레드 블로킹을 방지한다.
                commands.unlink(*result.keys.toTypedArray())
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
                commands.scan(cursor, ScanArgs.Builder.matches(pattern).limit(100L))
            count += result.keys.size
            cursor = result
        } while (!result.isFinished)
        return count
    }

    /**
     * NearCache 통계 스냅샷을 반환한다.
     * [LettuceNearCacheConfig.recordStats]가 true일 때 로컬 hit/miss/eviction이 유효하다.
     */
    override fun stats(): NearCacheStatistics {
        val caffeineStats = frontCache.stats()
        return DefaultNearCacheStatistics(
            localHits = caffeineStats?.hitCount() ?: 0L,
            localMisses = caffeineStats?.missCount() ?: 0L,
            localSize = localCacheSize(),
            localEvictions = caffeineStats?.evictionCount() ?: 0L,
            backHits = backHitCount.value,
            backMisses = backMissCount.value
        )
    }

    /**
     * 로컬 캐시(Caffeine) 통계. [LettuceNearCacheConfig.recordStats]가 true일 때만 유효한 값을 반환한다.
     */
    fun localStats(): CacheStats? = frontCache.stats()

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

    private fun setRedis(key: String, value: V): String? {
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

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private fun setRedisBulk(map: Map<String, V>) {
        val redisMap = map.entries.associate { (key, value) -> config.redisKey(key) to value }

        if (setArgsPx != null) {
            commands.multi {
                redisMap.forEach { (redisKey, value) ->
                    set(redisKey, value, setArgsPx)
                }
            }
        } else {
            val status = commands.mset(redisMap)
            check(status == "OK") { "Redis MSET failed for cacheName=${config.cacheName}: $status" }
        }
    }

    private fun registerTrackingKey(key: String) {
        // CLIENT TRACKING 활성화: 다른 인스턴스가 이 키를 수정할 때 invalidation을 받을 수 있도록
        // 개선: 사전 획득한 `asyncCommands` 필드를 사용해 getter 호출 비용을 제거한다.
        asyncCommands.get(config.redisKey(key))
    }

    private fun registerTrackingKeys(keys: Collection<String>) {
        if (keys.isEmpty()) return
        asyncCommands.mget(*keys.map(config::redisKey).toTypedArray())
    }
}
