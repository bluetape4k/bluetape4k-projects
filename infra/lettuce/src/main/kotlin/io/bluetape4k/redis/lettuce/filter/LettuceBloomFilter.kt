package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Redis BitSet 기반 Bloom Filter 동기 구현입니다.
 *
 * [tryInit]으로 필터 메타데이터를 먼저 기록한 뒤 [add], [contains]를 사용합니다.
 * 오탐은 발생할 수 있지만 미탐은 허용하지 않습니다.
 *
 * @property connection `StringCodec` 기반 Redis 연결
 * @property filterName Redis 키 prefix
 * @property options Bloom Filter 구성 옵션
 */
class LettuceBloomFilter(
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
    private val commands: RedisCommands<String, String> = connection.sync()

    /** 비트 배열 크기입니다. `m = ceil(-n * ln(p) / (ln2)^2)` */
    val m: Long = ceil(-options.expectedInsertions * ln(options.falseProbability) / ln(2.0).pow(2)).toLong()

    /** 해시 함수 수입니다. `k = round(m / n * ln2)` */
    val k: Int = (m.toDouble() / options.expectedInsertions * ln(2.0)).roundToInt().coerceAtLeast(1)

    /**
     * 필터 메타데이터를 초기화합니다.
     *
     * 이미 초기화된 필터를 같은 파라미터로 다시 초기화하면 `false`를 반환하고,
     * 다른 파라미터로 재초기화하려고 하면 [IllegalStateException]을 던집니다.
     */
    fun tryInit(): Boolean {
        val initialized = commands.hsetnx(configKey, "k", k.toString())
        if (initialized) {
            commands.hset(
                configKey,
                mapOf(
                    "m" to m.toString(),
                    "n" to options.expectedInsertions.toString(),
                    "p" to options.falseProbability.toString(),
                )
            )
            log.debug { "BloomFilter 초기화: name=$filterName, m=$m, k=$k" }
            return true
        }

        val storedM = commands.hget(configKey, "m")?.toLongOrNull()
        val storedK = commands.hget(configKey, "k")?.toIntOrNull()
        if (storedM != null && storedK != null && (storedM != m || storedK != k)) {
            throw IllegalStateException(
                "BloomFilter '$filterName' 이미 다른 파라미터로 초기화됨: 저장된 m=$storedM/k=$storedK, 현재 m=$m/k=$k"
            )
        }
        return false
    }

    /** 원소를 필터에 기록합니다. */
    fun add(element: String) {
        val positions = hashPositions(element)
        commands.eval<Long>(ADD_SCRIPT, ScriptOutputType.INTEGER, arrayOf(filterName), *positions)
        log.debug { "BloomFilter add: name=$filterName, element=$element" }
    }

    /** 원소의 존재 가능성을 조회합니다. */
    fun contains(element: String): Boolean {
        val positions = hashPositions(element)
        return commands.eval<Long>(
            CONTAINS_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(filterName),
            *positions
        ) == 1L
    }

    override fun close() = connection.close()

    private fun hashPositions(element: String): Array<String> {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, h2) = Murmur3.hash128x64(bytes)
        return Array(k) { index -> Math.floorMod(h1 + index.toLong() * h2, m).toString() }
    }
}
