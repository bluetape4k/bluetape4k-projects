package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

/**
 * 삭제를 지원하는 Redis 기반 Cuckoo Filter 코루틴 구현입니다.
 *
 * 동기 구현과 동일하게 undo-log 기반 Lua 스크립트로 삽입 실패 시 기존 상태를 복구합니다.
 */
class LettuceSuspendCuckooFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: CuckooFilterOptions = CuckooFilterOptions.Default,
): AutoCloseable {

    companion object: KLogging()

    private val bucketsKey = "$filterName:buckets"
    private val configKey = "$filterName:config"
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /** 전체 버킷 수입니다. */
    val numBuckets: Long = (options.capacity + options.bucketSize - 1) / options.bucketSize

    /** 필터 메타데이터를 초기화합니다. */
    suspend fun tryInit(): Boolean {
        val initialized = asyncCommands.hsetnx(configKey, "capacity", options.capacity.toString()).awaitSuspending()
        if (initialized) {
            asyncCommands.hset(
                configKey,
                mapOf(
                    "bucketSize" to options.bucketSize.toString(),
                    "numBuckets" to numBuckets.toString(),
                    "count" to "0",
                )
            ).awaitSuspending()
            log.debug { "SuspendCuckooFilter 초기화: name=$filterName" }
            return true
        }

        val storedCapacity = asyncCommands.hget(configKey, "capacity").awaitSuspending()?.toLongOrNull()
        val storedBucketSize = asyncCommands.hget(configKey, "bucketSize").awaitSuspending()?.toIntOrNull()
        val storedNumBuckets = asyncCommands.hget(configKey, "numBuckets").awaitSuspending()?.toLongOrNull()
        if (
            storedCapacity != null &&
            storedBucketSize != null &&
            storedNumBuckets != null &&
            (
                    storedCapacity != options.capacity ||
                            storedBucketSize != options.bucketSize ||
                            storedNumBuckets != numBuckets
                    )
        ) {
            throw IllegalStateException(
                "CuckooFilter '$filterName' 이미 다른 파라미터로 초기화됨: " +
                        "저장된 capacity=$storedCapacity/bucketSize=$storedBucketSize/numBuckets=$storedNumBuckets, " +
                        "현재 capacity=${options.capacity}/bucketSize=${options.bucketSize}/numBuckets=$numBuckets"
            )
        }
        return false
    }

    /** 원소를 삽입하고 성공 여부를 반환합니다. */
    suspend fun insert(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.INSERT,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString(),
            options.bucketSize.toString(),
            options.maxIterations.toString(),
            numBuckets.toString()
        ).awaitSuspending() == 1L
    }

    /** 원소가 존재하는지 검사합니다. */
    suspend fun contains(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.CONTAINS,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString()
        ).awaitSuspending() == 1L
    }

    /** 원소를 삭제하고 성공 여부를 반환합니다. */
    suspend fun delete(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return asyncCommands.eval<Long>(
            CuckooFilterScripts.DELETE,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString()
        ).awaitSuspending() == 1L
    }

    /** 현재 저장된 원소 수를 반환합니다. */
    suspend fun count(): Long = asyncCommands.hget(configKey, "count").awaitSuspending()?.toLongOrNull() ?: 0L

    override fun close() = connection.close()

    private data class FingerprintData(val fp: Int, val i1: Long, val i2: Long)

    private fun fingerprint(element: String): FingerprintData {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, _) = Murmur3.hash128x64(bytes)
        val fp = (kotlin.math.abs(h1.toInt()) % 255) + 1
        val i1 = Math.floorMod(h1, numBuckets) + 1
        val fpHash = kotlin.math.abs(fp.toLong() * 2654435761L) % numBuckets
        val i2 = Math.floorMod((i1 - 1) xor fpHash, numBuckets) + 1
        return FingerprintData(fp, i1, i2)
    }
}
