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
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.io.Closeable

/**
 * Lettuce(Redis) 기반 코루틴 네이티브 Read-through / Write-through / Write-behind Map.
 *
 * [LettuceLoadedMap]의 suspend 버전으로 `runBlocking` 없이 코루틴에서 직접 사용한다.
 *
 * - **Read-through**: 캐시 미스 시 [SuspendedMapLoader]를 통해 DB에서 값을 로드하고 Redis에 캐싱한다.
 * - **Write-through**: [SuspendedMapWriter]를 통해 DB에 즉시 쓰고 Redis도 갱신한다.
 * - **Write-behind**: [SuspendedMapWriter]를 통해 DB에 비동기로 쓰고 Redis는 즉시 갱신한다.
 * - **NONE**: Redis만 사용하고 DB 쓰기를 하지 않는다.
 *
 * ```kotlin
 * val loader = object : SuspendedMapLoader<String, MyData> {
 *     override suspend fun load(key: String): MyData? = db.findByKey(key)
 *     override suspend fun loadAllKeys(): List<String> = db.findAllKeys()
 * }
 * val writer = object : SuspendedMapWriter<String, MyData> {
 *     override suspend fun write(map: Map<String, MyData>) { db.upsertAll(map) }
 *     override suspend fun delete(keys: Collection<String>) { db.deleteAll(keys) }
 * }
 * val map = LettuceSuspendedLoadedMap<String, MyData>(redisClient, loader, writer)
 * val value = map.get("key")  // Redis 미스 시 DB 로드 (Read-through)
 * map.set("key", myData)      // Redis + DB에 즉시 쓰기 (Write-through)
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
 * @param scope 코루틴 스코프 (Write-behind Consumer 실행용)
 */
class LettuceSuspendedLoadedMap<K: Any, V: Any>(
    private val client: RedisClient,
    private val loader: SuspendedMapLoader<K, V>? = null,
    private val writer: SuspendedMapWriter<K, V>? = null,
    private val config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
    private val keySerializer: (K) -> String = { it.toString() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
): Closeable {
    companion object: KLogging() {
        private const val MAX_DEAD_LETTER_RETRY = 3
    }

    @Suppress("UNCHECKED_CAST")
    private val codec = LettuceBinaryCodec<V>(BinarySerializers.LZ4Fory)

    private val connection: StatefulRedisConnection<String, V> = client.connect(codec)
    private val asyncCommands: RedisAsyncCommands<String, V> = connection.async()

    private val lazyStrConnection = lazy { client.connect(StringCodec.UTF8) }
    private val strAsyncCommands by lazy { lazyStrConnection.value.async() }

    private val ttlSeconds = config.ttl.seconds

    // Write-behind: Channel + coroutine consumer
    private val writeBehindChannel: Channel<Triple<K, V, Int>>? =
        if (config.writeMode == WriteMode.WRITE_BEHIND) {
            Channel(config.writeBehindQueueCapacity)
        } else {
            null
        }

    private val writeBehindJob =
        writeBehindChannel?.let {
            scope.launch { consumeWriteBehindChannel() }
        }

    private fun redisKey(key: K): String = "${config.keyPrefix}:${keySerializer(key)}"

    /**
     * 캐시에서 값을 조회한다. 캐시 미스 시 [SuspendedMapLoader]를 통해 DB에서 로드한다.
     *
     * ```kotlin
     * val value = map.get("key")
     * // Redis에 있으면 캐시에서, 없으면 DB에서 로드
     * // 값이 없으면 null
     * ```
     *
     * @param key 조회할 키
     * @return 값 (없으면 null)
     */
    suspend fun get(key: K): V? {
        val redisKey = redisKey(key)
        val cached = runCatching { asyncCommands.get(redisKey).await() }.getOrNull()
        if (cached != null) return cached
        val loader = loader ?: return null
        val value = loader.load(key) ?: return null
        runCatching { asyncCommands.set(redisKey, value, SetArgs().ex(ttlSeconds)).await() }
            .onFailure { log.warn(it) { "Redis SETEX 실패: $redisKey" } }
        return value
    }

    /**
     * 캐시에 값을 저장한다. [config.writeMode]에 따라 DB에도 반영한다.
     *
     * ```kotlin
     * map.set("key", myData)
     * // WriteMode.WRITE_THROUGH: Redis + DB에 즉시 반영
     * // WriteMode.WRITE_BEHIND: Redis 즉시 반영, DB는 비동기
     * ```
     *
     * @param key 저장할 키
     * @param value 저장할 값
     */
    suspend fun set(
        key: K,
        value: V,
    ) {
        when (config.writeMode) {
            WriteMode.NONE          -> {
                asyncCommands.set(redisKey(key), value, SetArgs().ex(ttlSeconds)).await()
            }
            WriteMode.WRITE_THROUGH -> {
                writer?.write(mapOf(key to value))
                asyncCommands.set(redisKey(key), value, SetArgs().ex(ttlSeconds)).await()
            }
            WriteMode.WRITE_BEHIND  -> {
                val channel = writeBehindChannel ?: return
                val result = channel.trySend(Triple(key, value, 0))
                if (result.isFailure) {
                    throw IllegalStateException(
                        "Write-behind 채널 포화 (capacity=${config.writeBehindQueueCapacity})"
                    )
                }
                asyncCommands.set(redisKey(key), value, SetArgs().ex(ttlSeconds)).await()
            }
        }
    }

    /**
     * 여러 키의 값을 일괄 조회한다. 캐시 미스 키는 [SuspendedMapLoader]로 로드한다.
     */
    suspend fun getAll(keys: Set<K>): Map<K, V> {
        if (keys.isEmpty()) return emptyMap()
        val keyList = keys.toList()
        val redisKeys = keyList.map { redisKey(it) }.toTypedArray()

        val values = runCatching { asyncCommands.mget(*redisKeys).await() }.getOrNull() ?: emptyList()

        val result = mutableMapOf<K, V>()
        val missedKeys = mutableListOf<K>()

        values.forEachIndexed { i, kv ->
            if (kv != null && kv.hasValue()) {
                result[keyList[i]] = kv.value
            } else {
                missedKeys.add(keyList[i])
            }
        }

        if (missedKeys.isNotEmpty() && loader != null) {
            for (key in missedKeys) {
                val value = loader.load(key) ?: continue
                result[key] = value
                runCatching {
                    asyncCommands.set(redisKey(key), value, SetArgs().ex(ttlSeconds)).await()
                }.onFailure { log.warn(it) { "Redis SETEX 실패: ${redisKey(key)}" } }
            }
        }
        return result
    }

    /**
     * 캐시에서 값을 삭제한다. WRITE_THROUGH/WRITE_BEHIND 모드에서는 DB에서도 삭제한다.
     */
    suspend fun delete(key: K) {
        if (config.writeMode != WriteMode.NONE) writer?.delete(listOf(key))
        asyncCommands.del(redisKey(key)).await()
    }

    /**
     * 여러 키의 값을 일괄 삭제한다.
     */
    suspend fun deleteAll(keys: Collection<K>) {
        if (keys.isEmpty()) return
        if (config.writeMode != WriteMode.NONE) writer?.delete(keys)
        asyncCommands.del(*keys.map { redisKey(it) }.toTypedArray()).await()
    }

    /**
     * 이 맵의 모든 Redis 키를 삭제한다.
     */
    suspend fun clear() {
        val pattern = "${config.keyPrefix}:*"
        val scanArgs = ScanArgs.Builder.matches(pattern).limit(100)
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val scanResult = asyncCommands.scan(cursor, scanArgs).await()
            if (scanResult.keys.isNotEmpty()) {
                asyncCommands.del(*scanResult.keys.toTypedArray()).await()
            }
            cursor = scanResult
        } while (!cursor.isFinished)
    }

    private suspend fun consumeWriteBehindChannel() {
        val channel = writeBehindChannel ?: return
        while (currentCoroutineContext().isActive) {
            val batch = mutableListOf<Triple<K, V, Int>>()
            // 첫 아이템은 blocking receive
            val first = channel.receiveCatching().getOrNull() ?: break
            batch.add(first)
            // 나머지는 non-blocking tryReceive로 batch 수집
            while (batch.size < config.writeBehindBatchSize) {
                val next = channel.tryReceive().getOrNull() ?: break
                batch.add(next)
            }
            flushBatch(batch)
            delay(config.writeBehindDelay)
        }
    }

    private suspend fun flushBatch(entries: List<Triple<K, V, Int>>) {
        if (entries.isEmpty()) return
        val batch = entries.associate { it.first to it.second }
        runCatching { writer?.write(batch) }
            .onFailure { e ->
                val retryCount = entries.first().third + 1
                log.error(e) { "Write-behind flush 실패 (attempt $retryCount): ${batch.keys}" }
                if (retryCount < MAX_DEAD_LETTER_RETRY) {
                    entries.forEach { (k, v, _) ->
                        writeBehindChannel?.trySend(Triple(k, v, retryCount))
                    }
                } else {
                    runCatching {
                        val deadLetterKey = "${config.keyPrefix}:dead-letter"
                        strAsyncCommands
                            .lpush(deadLetterKey, *batch.keys.map { keySerializer(it) }.toTypedArray())
                            .await()
                    }.onFailure { ex -> log.error(ex) { "Dead letter 기록 실패" } }
                }
            }
    }

    override fun close() {
        writeBehindJob?.cancel()
        writeBehindChannel?.let { channel ->
            // 남은 항목 flush (shutdown 시)
            val deadline = System.currentTimeMillis() + config.writeBehindShutdownTimeout.toMillis()
            while (!channel.isEmpty && System.currentTimeMillis() < deadline) {
                val batch = mutableListOf<Triple<K, V, Int>>()
                while (batch.size < config.writeBehindBatchSize) {
                    val item = channel.tryReceive().getOrNull() ?: break
                    batch.add(item)
                }
                if (batch.isEmpty()) break
                runBlocking { flushBatch(batch) }
            }
            if (!channel.isEmpty) {
                log.warn { "Write-behind shutdown 타임아웃: 일부 항목 유실" }
            }
            channel.close()
        }
        scope.cancel()
        if (lazyStrConnection.isInitialized()) lazyStrConnection.value.close()
        connection.close()
    }
}
