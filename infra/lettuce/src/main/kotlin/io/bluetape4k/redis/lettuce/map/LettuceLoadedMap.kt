package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Lettuce(Redis) 기반 Read-through / Write-through / Write-behind Map.
 *
 * - **Read-through**: 캐시 미스 시 [MapLoader]를 통해 DB에서 값을 로드하고 Redis에 캐싱한다.
 * - **Write-through**: [MapWriter]를 통해 DB에 즉시 쓰고 Redis도 갱신한다.
 * - **Write-behind**: [MapWriter]를 통해 DB에 비동기로 쓰고 Redis는 즉시 갱신한다.
 * - **NONE**: Redis만 사용하고 DB 쓰기를 하지 않는다.
 *
 * ```kotlin
 * val loader = object : MapLoader<String, MyData> {
 *     override fun load(key: String): MyData? = db.findByKey(key)
 *     override fun loadAllKeys(): Iterable<String> = db.findAllKeys()
 * }
 * val writer = object : MapWriter<String, MyData> {
 *     override fun write(map: Map<String, MyData>) { db.upsertAll(map) }
 *     override fun delete(keys: Collection<String>) { db.deleteAll(keys) }
 * }
 * val map = LettuceLoadedMap<String, MyData>(redisClient, loader, writer)
 * val value = map["key"]      // Redis 미스 시 DB 로드 (Read-through)
 * map["key"] = myData         // Redis + DB에 즉시 쓰기 (Write-through)
 * map.delete("key")           // Redis + DB에서 삭제
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param client Lettuce [RedisClient]
 * @param loader DB 로드 함수 (Read-through, null이면 캐시 전용)
 * @param writer DB 쓰기 함수 (Write-through / Write-behind, null이면 쓰기 없음)
 * @param config [LettuceCacheConfig] 설정
 * @param keySerializer K → String 변환 함수 (기본: toString())
 */
class LettuceLoadedMap<K: Any, V: Any>(
    private val client: RedisClient,
    private val loader: MapLoader<K, V>? = null,
    private val writer: MapWriter<K, V>? = null,
    private val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
    private val keySerializer: (K) -> String = { it.toString() },
): Closeable {
    companion object: KLogging() {
        private const val MAX_DEAD_LETTER_RETRY = 3
    }

    @Suppress("UNCHECKED_CAST")
    private val codec = LettuceBinaryCodec<V>(BinarySerializers.LZ4Fory)

    private val connection: StatefulRedisConnection<String, V> = client.connect(codec)
    private val commands: RedisCommands<String, V> = connection.sync()

    private val lazyStrConnection = lazy { client.connect(StringCodec.UTF8) }
    private val strConnection: StatefulRedisConnection<String, String> by lazyStrConnection
    private val strCommands: RedisCommands<String, String> by lazy { strConnection.sync() }

    private val ttlSeconds = config.ttl.seconds

    private val writeBehindQueue: LinkedBlockingDeque<Triple<K, V, Int>>? =
        if (config.writeMode == WriteMode.WRITE_BEHIND) {
            LinkedBlockingDeque(config.writeBehindQueueCapacity)
        } else {
            null
        }

    private val scheduler: ScheduledExecutorService? =
        if (config.writeMode == WriteMode.WRITE_BEHIND) {
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "lettuce-write-behind-flusher").also { it.isDaemon = true }
            }
        } else {
            null
        }

    init {
        scheduler?.scheduleWithFixedDelay(
            ::flushWriteBehindQueue,
            config.writeBehindDelay.toMillis(),
            config.writeBehindDelay.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    private fun redisKey(key: K): String = "${config.keyPrefix}:${keySerializer(key)}"

    /**
     * 키에 해당하는 값을 반환합니다.
     * Redis 미스 시 [MapLoader]를 통해 DB에서 로드합니다.
     *
     * ```kotlin
     * val value = map["key"]
     * // Redis에 있으면 캐시에서, 없으면 DB에서 로드
     * // 값이 없으면 null
     * ```
     *
     * @param key 조회할 키
     * @return 값 (없으면 null)
     */
    operator fun get(key: K): V? {
        val redisKey = redisKey(key)
        val cached = runCatching { commands.get(redisKey) }
            .onFailure { log.warn(it) { "Redis GET 실패, loader fallback: $redisKey" } }
            .getOrNull()
        if (cached != null) return cached
        val loader = loader ?: return null
        val value = loader.load(key) ?: return null
        runCatching { commands.set(redisKey, value, SetArgs().ex(ttlSeconds)) }
            .onFailure { log.warn(it) { "Redis SETEX 실패: $redisKey" } }
        return value
    }

    /**
     * 키-값 쌍을 저장합니다.
     * [LettuceCacheConfig.writeMode]에 따라 Write-through 또는 Write-behind로 DB에도 반영합니다.
     *
     * ```kotlin
     * map["key"] = myData
     * // WriteMode.WRITE_THROUGH: Redis + DB에 즉시 반영
     * // WriteMode.WRITE_BEHIND: Redis 즉시 반영, DB는 비동기
     * ```
     *
     * @param key 저장할 키
     * @param value 저장할 값
     */
    operator fun set(
        key: K,
        value: V,
    ) {
        when (config.writeMode) {
            WriteMode.NONE          -> {
                commands.set(redisKey(key), value, SetArgs().ex(ttlSeconds))
            }
            WriteMode.WRITE_THROUGH -> {
                writer?.write(mapOf(key to value))
                commands.set(redisKey(key), value, SetArgs().ex(ttlSeconds))
            }
            WriteMode.WRITE_BEHIND  -> {
                val queue = writeBehindQueue ?: return
                if (!queue.offer(Triple(key, value, 0))) {
                    throw IllegalStateException(
                        "Write-behind 큐 포화 (capacity=${config.writeBehindQueueCapacity})"
                    )
                }
                commands.set(redisKey(key), value, SetArgs().ex(ttlSeconds))
            }
        }
    }

    fun getAll(keys: Set<K>): Map<K, V> {
        if (keys.isEmpty()) return emptyMap()
        val keyList = keys.toList()
        val redisKeys = keyList.map { redisKey(it) }.toTypedArray()

        val mgetResult = runCatching { commands.mget(*redisKeys) }
            .onFailure { log.warn(it) { "Redis MGET 실패, loader fallback: ${redisKeys.take(5)}..." } }
            .getOrNull()

        val result = mutableMapOf<K, V>()
        val missedKeys: MutableList<K>

        if (mgetResult == null) {
            // MGET failed entirely — treat all requested keys as cache misses
            missedKeys = keyList.toMutableList()
        } else {
            missedKeys = mutableListOf()
            mgetResult.forEachIndexed { i, kv ->
                if (kv != null && kv.hasValue()) {
                    result[keyList[i]] = kv.value
                } else {
                    missedKeys.add(keyList[i])
                }
            }
        }

        if (missedKeys.isNotEmpty() && loader != null) {
            for (key in missedKeys) {
                val value = loader.load(key) ?: continue
                result[key] = value
                runCatching { commands.set(redisKey(key), value, SetArgs().ex(ttlSeconds)) }
                    .onFailure { log.warn(it) { "Redis SETEX 실패: ${redisKey(key)}" } }
            }
        }
        return result
    }

    /**
     * 키를 Redis 및 DB에서 삭제합니다.
     *
     * ```kotlin
     * map.delete("key")
     * val value = map["key"]
     * // value == null
     * ```
     *
     * @param key 삭제할 키
     */
    fun delete(key: K) {
        if (config.writeMode != WriteMode.NONE) writer?.delete(listOf(key))
        commands.del(redisKey(key))
    }

    /**
     * 여러 키를 Redis 및 DB에서 일괄 삭제합니다.
     *
     * ```kotlin
     * map.deleteAll(listOf("key1", "key2"))
     * ```
     *
     * @param keys 삭제할 키 컬렉션
     */
    fun deleteAll(keys: Collection<K>) {
        if (keys.isEmpty()) return
        if (config.writeMode != WriteMode.NONE) writer?.delete(keys)
        // 개선: 대량 삭제는 UNLINK(비동기 해제)로 전환해 Redis 이벤트루프 블로킹을 최소화합니다.
        commands.unlink(*keys.map { redisKey(it) }.toTypedArray())
    }

    /**
     * Redis 캐시에서만 해당 키를 제거합니다 (DB에는 영향 없음).
     *
     * @param key 캐시에서 제거할 키
     */
    fun evict(key: K) {
        commands.del(redisKey(key))
    }

    /**
     * Redis 캐시에서만 여러 키를 제거합니다 (DB에는 영향 없음).
     *
     * @param keys 캐시에서 제거할 키 컬렉션
     */
    fun evictAll(keys: Collection<K>) {
        if (keys.isEmpty()) return
        // 개선: 캐시 대량 만료는 UNLINK(비동기 해제)로 전환.
        commands.unlink(*keys.map { redisKey(it) }.toTypedArray())
    }

    /**
     * 패턴에 일치하는 키를 Redis SCAN으로 찾아 삭제합니다.
     *
     * @param patterns 키 패턴 (예: "*user*", "prefix:*")
     * @param count 한 번에 스캔할 키 수
     * @return 삭제된 항목 수
     */
    fun invalidateByPattern(patterns: String, count: Long = 100L): Long {
        val keyPattern = "${config.keyPrefix}:$patterns"
        val scanArgs = ScanArgs.Builder.matches(keyPattern).limit(count)
        var cursor: ScanCursor = ScanCursor.INITIAL
        var deleted = 0L
        do {
            val scanResult = commands.scan(cursor, scanArgs)
            if (scanResult.keys.isNotEmpty()) {
                // 개선: SCAN + DEL → SCAN + UNLINK 로 대용량 invalidate 비차단 처리.
                deleted += commands.unlink(*scanResult.keys.toTypedArray())
            }
            cursor = scanResult
        } while (!cursor.isFinished)
        return deleted
    }

    /**
     * keyPrefix에 해당하는 모든 Redis 항목을 삭제합니다.
     *
     * ```kotlin
     * map.clear()
     * val value = map["key"]
     * // value == null (전체 삭제 후)
     * ```
     */
    fun clear() {
        val pattern = "${config.keyPrefix}:*"
        val scanArgs = ScanArgs.Builder.matches(pattern).limit(100)
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val scanResult = commands.scan(cursor, scanArgs)
            if (scanResult.keys.isNotEmpty()) {
                // 개선: clear() 도 UNLINK 적용해 대량 키 삭제의 블로킹을 제거.
                commands.unlink(*scanResult.keys.toTypedArray())
            }
            cursor = scanResult
        } while (!cursor.isFinished)
    }

    private fun flushWriteBehindQueue() {
        val queue = writeBehindQueue ?: return
        val entries = mutableListOf<Triple<K, V, Int>>()
        var count = 0
        while (count < config.writeBehindBatchSize) {
            val entry = queue.poll() ?: break
            entries.add(entry)
            count++
        }
        if (entries.isEmpty()) return

        val batch = entries.associate { it.first to it.second }
        runCatching { writer?.write(batch) }
            .onFailure { e ->
                val attempts = entries.map { it.third + 1 }
                log.error(e) { "Write-behind flush 실패 (attempts=$attempts): ${batch.keys}" }
                val failed = mutableListOf<Triple<K, V, Int>>()
                entries.forEach { (k, v, retryCount) ->
                    val nextRetryCount = retryCount + 1
                    if (nextRetryCount < MAX_DEAD_LETTER_RETRY) {
                        // 개선: offerFirst 반환값 확인 — 큐가 가득 찬 경우 재시도 항목이 유실될 수 있음.
                        //       유실 항목은 dead-letter로 즉시 처리합니다.
                        val offered = queue.offerFirst(Triple(k, v, nextRetryCount))
                        if (!offered) {
                            log.warn { "재시도 큐 포화 — dead-letter 전송: key=$k (attempt=$nextRetryCount)" }
                            failed.add(Triple(k, v, nextRetryCount))
                        }
                    } else {
                        failed.add(Triple(k, v, nextRetryCount))
                    }
                }
                if (failed.isNotEmpty()) {
                    runCatching {
                        // 개선: 기존엔 키만 LPUSH 했기 때문에 실패한 엔트리 복구가 불가능했습니다.
                        //       이제 HSET(복구용 값)을 먼저 저장 후 LPUSH(모니터링용 키 목록)를 저장합니다.
                        //       두 명령은 서로 다른 codec connection 사용으로 MULTI/EXEC 불가.
                        //       HSET 실패 시 LPUSH를 건너뛰어 복구 불가 상태(모니터링 오탐)를 방지합니다.
                        val deadLetterKey = "${config.keyPrefix}:dead-letter"
                        val deadLetterValuesKey = "${config.keyPrefix}:dead-letter:values"
                        val failedBatch = failed.associate { it.first to it.second }
                        val valueMap = failedBatch.entries.associate { (k, v) -> keySerializer(k) to v }
                        // HSET 먼저: 실패 시 LPUSH 건너뜀 (복구 데이터 우선)
                        commands.hset(deadLetterValuesKey, valueMap)
                        val serializedKeys = failedBatch.keys.map { keySerializer(it) }
                        strCommands.lpush(deadLetterKey, *serializedKeys.toTypedArray())
                    }.onFailure { ex -> log.error(ex) { "Dead letter 기록 실패" } }
                }
            }
    }

    override fun close() {
        scheduler?.let { sched ->
            sched.shutdown()
            val deadline = System.currentTimeMillis() + config.writeBehindShutdownTimeout.toMillis()
            while (writeBehindQueue?.isNotEmpty() == true && System.currentTimeMillis() < deadline) {
                flushWriteBehindQueue()
            }
            if (writeBehindQueue?.isNotEmpty() == true) {
                log.warn { "Write-behind shutdown 타임아웃: ${writeBehindQueue.size}개 항목 유실" }
            }
            sched.awaitTermination(1, TimeUnit.SECONDS)
        }
        if (lazyStrConnection.isInitialized()) strConnection.close()
        connection.close()
    }
}
