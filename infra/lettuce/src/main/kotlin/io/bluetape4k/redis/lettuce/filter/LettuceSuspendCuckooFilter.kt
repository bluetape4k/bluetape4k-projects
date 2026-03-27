package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await

/**
 * Lettuce 기반 분산 Cuckoo Filter (Coroutine/Suspend).
 *
 * [LettuceCuckooFilter]의 코루틴 버전입니다.
 * [CuckooFilterScripts]의 동일한 Lua 스크립트(undo-log 롤백 포함)를 사용합니다.
 *
 * @param connection StringCodec 기반 연결
 * @param filterName 필터 이름 (Redis 키 prefix)
 * @param options CuckooFilterOptions
 */
class LettuceSuspendCuckooFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: CuckooFilterOptions = CuckooFilterOptions.Default,
) : AutoCloseable {

    companion object : KLogging()

    private val bucketsKey = "$filterName:buckets"
    private val configKey = "$filterName:config"

    /** 버킷 수 = ceil(capacity / bucketSize) */
    val numBuckets: Long = (options.capacity + options.bucketSize - 1) / options.bucketSize

    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /**
     * 필터를 초기화합니다. 이미 초기화된 경우 false를 반환합니다 (NX 멱등성).
     */
    suspend fun tryInit(): Boolean {
        val set = asyncCommands.hsetnx(configKey, "capacity", options.capacity.toString()).await()
        if (set) {
            asyncCommands.hset(
                configKey, mapOf(
                    "bucketSize" to options.bucketSize.toString(),
                    "numBuckets" to numBuckets.toString(),
                    "count" to "0",
                )
            ).await()
            log.debug { "SuspendCuckooFilter 초기화: name=$filterName, numBuckets=$numBuckets" }
        }
        return set
    }

    /**
     * 원소를 삽입합니다. 성공 시 true, 필터 포화 시 false.
     * 실패 시 기존 원소는 유실되지 않습니다 (undo-log 롤백).
     */
    suspend fun insert(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.INSERT, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString(),
            options.bucketSize.toString(), options.maxIterations.toString(), numBuckets.toString()
        ).await() == 1L
    }

    /**
     * 원소가 필터에 포함되어 있는지 확인합니다.
     */
    suspend fun contains(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.CONTAINS, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString()
        ).await() == 1L
    }

    /**
     * 원소를 삭제합니다. 존재하지 않으면 false.
     */
    suspend fun delete(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.DELETE, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString()
        ).await() == 1L
    }

    /**
     * 필터에 삽입된 원소 수 (근사).
     */
    suspend fun count(): Long = asyncCommands.hget(configKey, "count").await()?.toLongOrNull() ?: 0L

    override fun close() = connection.close()

    private data class FingerprintData(val fp: Int, val i1: Long, val i2: Long)

    private fun fingerprint(element: String): FingerprintData {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, _) = Murmur3.hash128x64(bytes)

        val fp = (Math.abs(h1.toInt()) % 255) + 1
        val i1 = Math.floorMod(h1, numBuckets) + 1
        val fpHash = Math.abs(fp.toLong() * 2654435761L) % numBuckets
        val i2 = Math.floorMod((i1 - 1) xor fpHash, numBuckets) + 1

        return FingerprintData(fp, i1, i2)
    }
}
