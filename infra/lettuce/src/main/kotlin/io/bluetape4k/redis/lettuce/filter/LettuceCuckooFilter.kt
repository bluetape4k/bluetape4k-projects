package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

/**
 * 삭제를 지원하는 Redis 기반 Cuckoo Filter 동기 구현입니다.
 *
 * 삽입 중 kick-out 재배치가 실패하면 Lua undo-log로 기존 버킷 상태를 복구합니다.
 *
 * ```kotlin
 * val filter = LettuceCuckooFilter(connection, "my-filter", CuckooFilterOptions(capacity = 1000))
 * filter.tryInit()
 * filter.insert("apple")
 * val exists = filter.contains("apple")
 * // exists == true
 * filter.delete("apple")
 * val count = filter.count()
 * // count == 0
 * ```
 *
 * @property connection `StringCodec` 기반 Redis 연결
 * @property filterName Redis 키 prefix
 * @property options Cuckoo Filter 구성 옵션
 */
class LettuceCuckooFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: CuckooFilterOptions = CuckooFilterOptions.Default,
): AutoCloseable {

    companion object: KLogging()

    private val bucketsKey = "$filterName:buckets"
    private val configKey = "$filterName:config"
    private val commands: RedisCommands<String, String> = connection.sync()

    /** 전체 버킷 수입니다. */
    val numBuckets: Long = (options.capacity + options.bucketSize - 1) / options.bucketSize

    /**
     * 필터 메타데이터를 초기화합니다.
     *
     * 이미 동일 이름의 필터가 있으면 저장된 구성과 현재 구성이 같은지 검증합니다.
     */
    fun tryInit(): Boolean {
        val initialized = commands.hsetnx(configKey, "capacity", options.capacity.toString())
        if (initialized) {
            commands.hset(
                configKey,
                mapOf(
                    "bucketSize" to options.bucketSize.toString(),
                    "numBuckets" to numBuckets.toString(),
                    "count" to "0",
                )
            )
            log.debug { "CuckooFilter 초기화: name=$filterName, numBuckets=$numBuckets" }
            return true
        }

        val storedCapacity = commands.hget(configKey, "capacity")?.toLongOrNull()
        val storedBucketSize = commands.hget(configKey, "bucketSize")?.toIntOrNull()
        val storedNumBuckets = commands.hget(configKey, "numBuckets")?.toLongOrNull()
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

    /**
     * 원소를 삽입하고 성공 여부를 반환합니다.
     *
     * ```kotlin
     * val ok = filter.insert("apple")
     * // ok == true
     * ```
     */
    fun insert(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.INSERT,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString(),
            options.bucketSize.toString(),
            options.maxIterations.toString(),
            numBuckets.toString()
        ) == 1L
    }

    /**
     * 원소가 존재하는지 검사합니다.
     *
     * ```kotlin
     * filter.insert("apple")
     * val exists = filter.contains("apple")
     * // exists == true
     * val notExists = filter.contains("banana")
     * // notExists == false
     * ```
     */
    fun contains(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.CONTAINS,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString()
        ) == 1L
    }

    /**
     * 원소를 삭제하고 성공 여부를 반환합니다.
     *
     * ```kotlin
     * filter.insert("apple")
     * val deleted = filter.delete("apple")
     * // deleted == true
     * val notDeleted = filter.delete("apple")
     * // notDeleted == false (이미 삭제됨)
     * ```
     */
    fun delete(element: String): Boolean {
        val (fp, i1, i2) = fingerprint(element)
        return commands.eval<Long>(
            CuckooFilterScripts.DELETE,
            ScriptOutputType.INTEGER,
            arrayOf(bucketsKey, configKey),
            fp.toString(),
            i1.toString(),
            i2.toString()
        ) == 1L
    }

    /**
     * 현재 저장된 원소 수를 반환합니다.
     *
     * ```kotlin
     * filter.insert("apple")
     * filter.insert("banana")
     * val count = filter.count()
     * // count == 2
     * ```
     */
    fun count(): Long = commands.hget(configKey, "count")?.toLongOrNull() ?: 0L

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
