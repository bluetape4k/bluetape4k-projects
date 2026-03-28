package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Redis BitSet 기반 Bloom Filter 코루틴 구현입니다.
 *
 * 동기 구현과 동일한 메타데이터/오탐 규칙을 유지하면서 `RedisFuture`를 suspend로 감쌉니다.
 */
class LettuceSuspendBloomFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: BloomFilterOptions = BloomFilterOptions.Default,
): AutoCloseable {

    companion object: KLogging() {
        private const val ADD_SCRIPT = """
for i = 1, #ARGV do
    redis.call('setbit', KEYS[1], ARGV[i], 1)
end
return 1"""

        private const val CONTAINS_SCRIPT = """
for i = 1, #ARGV do
    if redis.call('getbit', KEYS[1], ARGV[i]) == 0 then return 0 end
end
return 1"""
    }

    private val configKey = "$filterName:config"
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /** 비트 배열 크기입니다. `m = ceil(-n * ln(p) / (ln2)^2)` */
    val m: Long = ceil(-options.expectedInsertions * ln(options.falseProbability) / ln(2.0).pow(2)).toLong()

    /** 해시 함수 수입니다. `k = round(m / n * ln2)` */
    val k: Int = (m.toDouble() / options.expectedInsertions * ln(2.0)).roundToInt().coerceAtLeast(1)

    /** 필터 메타데이터를 초기화합니다. */
    suspend fun tryInit(): Boolean {
        val initialized = asyncCommands.hsetnx(configKey, "k", k.toString()).awaitSuspending()
        if (initialized) {
            asyncCommands.hset(
                configKey,
                mapOf(
                    "m" to m.toString(),
                    "n" to options.expectedInsertions.toString(),
                    "p" to options.falseProbability.toString(),
                )
            ).awaitSuspending()
            log.debug { "SuspendBloomFilter 초기화: name=$filterName, m=$m, k=$k" }
            return true
        }

        val storedM = asyncCommands.hget(configKey, "m").awaitSuspending()?.toLongOrNull()
        val storedK = asyncCommands.hget(configKey, "k").awaitSuspending()?.toIntOrNull()
        if (storedM != null && storedK != null && (storedM != m || storedK != k)) {
            throw IllegalStateException(
                "BloomFilter '$filterName' 이미 다른 파라미터로 초기화됨: 저장된 m=$storedM/k=$storedK, 현재 m=$m/k=$k"
            )
        }
        return false
    }

    /** 원소를 필터에 기록합니다. */
    suspend fun add(element: String) {
        val positions = hashPositions(element)
        asyncCommands.eval<Long>(
            ADD_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(filterName),
            *positions
        ).awaitSuspending()
    }

    /** 원소의 존재 가능성을 조회합니다. */
    suspend fun contains(element: String): Boolean {
        val positions = hashPositions(element)
        return asyncCommands.eval<Long>(
            CONTAINS_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(filterName),
            *positions
        ).awaitSuspending() == 1L
    }

    override fun close() = connection.close()

    private fun hashPositions(element: String): Array<String> {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, h2) = Murmur3.hash128x64(bytes)
        return Array(k) { index -> Math.floorMod(h1 + index.toLong() * h2, m).toString() }
    }
}
