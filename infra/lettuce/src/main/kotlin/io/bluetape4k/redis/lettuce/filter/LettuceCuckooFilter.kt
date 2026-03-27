package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

/**
 * Lettuce 기반 분산 Cuckoo Filter (Sync).
 *
 * BloomFilter와 달리 원소 **삭제**를 지원합니다. 오탐 가능, 미탐 없음.
 * fingerprint 이중 버킷 방식으로 원소를 Redis Hash에 저장합니다.
 *
 * kick-out 재배치 실패 시 undo-log로 원래 상태를 복구하여
 * 기존 원소 유실(거짓 음성) 없이 안전하게 실패합니다.
 *
 * **주의:** 동일 원소의 중복 삽입이 가능합니다. 삭제는 하나의 fingerprint만 제거합니다.
 *
 * @param connection StringCodec 기반 연결
 * @param filterName 필터 이름 (Redis 키 prefix)
 * @param options CuckooFilterOptions
 */
class LettuceCuckooFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: CuckooFilterOptions = CuckooFilterOptions.Default,
) : AutoCloseable {

    companion object : KLogging()

    private val bucketsKey = "$filterName:buckets"
    private val configKey = "$filterName:config"

    /** 버킷 수 = ceil(capacity / bucketSize) */
    val numBuckets: Long = (options.capacity + options.bucketSize - 1) / options.bucketSize

    private val commands: RedisCommands<String, String> = connection.sync()

    /**
     * 필터를 초기화합니다. 이미 초기화된 경우 false를 반환합니다 (NX 멱등성).
     */
    fun tryInit(): Boolean {
        val set = commands.hsetnx(configKey, "capacity", options.capacity.toString())
        if (set) {
            commands.hset(
                configKey, mapOf(
                    "bucketSize" to options.bucketSize.toString(),
                    "numBuckets" to numBuckets.toString(),
                    "count" to "0",
                )
            )
            log.debug { "CuckooFilter 초기화: name=$filterName, numBuckets=$numBuckets" }
        }
        return set
    }

    /**
     * 원소를 삽입합니다. 성공 시 true, 필터 포화 시 false.
     * 실패 시 기존 원소는 유실되지 않습니다 (undo-log 롤백).
     */
    fun insert(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.INSERT, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString(),
            options.bucketSize.toString(), options.maxIterations.toString(), numBuckets.toString()
        ) == 1L
    }

    /**
     * 원소가 필터에 포함되어 있는지 확인합니다.
     */
    fun contains(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.CONTAINS, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString()
        ) == 1L
    }

    /**
     * 원소를 삭제합니다. 존재하지 않으면 false.
     */
    fun delete(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.DELETE, ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(), i1.toString(), i2.toString()
        ) == 1L
    }

    /**
     * 필터에 삽입된 원소 수 (근사).
     */
    fun count(): Long = commands.hget(configKey, "count")?.toLongOrNull() ?: 0L

    override fun close() = connection.close()

    private data class FingerprintData(val fp: Int, val i1: Long, val i2: Long)

    private fun fingerprint(element: String): FingerprintData {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, _) = Murmur3.hash128x64(bytes)

        val fp = (Math.abs(h1.toInt()) % 255) + 1            // 1..255
        val i1 = Math.floorMod(h1, numBuckets) + 1           // 1-based
        val fpHash = Math.abs(fp.toLong() * 2654435761L) % numBuckets
        val i2 = Math.floorMod((i1 - 1) xor fpHash, numBuckets) + 1  // 1-based

        return FingerprintData(fp, i1, i2)
    }
}
