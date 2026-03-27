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
 * Lettuce 기반 분산 Bloom Filter (Sync).
 *
 * MurmurHash3 128bit로 k개 비트 위치를 계산하여 Redis BitSet에 저장합니다.
 * [tryInit] 호출로 초기화 후 [add]/[contains]를 사용합니다.
 *
 * **오탐(false positive):** 가능. **미탐(false negative):** 불가능.
 * **원소 삭제:** 지원하지 않음 (삭제가 필요하면 [LettuceCuckooFilter] 사용).
 *
 * @param connection StringCodec 기반 연결
 * @param filterName 필터 이름 (Redis 키 prefix)
 * @param options BloomFilterOptions
 */
class LettuceBloomFilter(
    private val connection: StatefulRedisConnection<String, String>,
    val filterName: String,
    val options: BloomFilterOptions = BloomFilterOptions.Default,
) : AutoCloseable {

    companion object : KLogging() {
        // KEYS[1]=bitsetKey / ARGV[1..k]=bit positions (문자열)
        private const val ADD_SCRIPT = """
for i = 1, #ARGV do
    redis.call('setbit', KEYS[1], ARGV[i], 1)
end
return 1"""

        // KEYS[1]=bitsetKey / ARGV[1..k]=bit positions
        private const val CONTAINS_SCRIPT = """
for i = 1, #ARGV do
    if redis.call('getbit', KEYS[1], ARGV[i]) == 0 then return 0 end
end
return 1"""
    }

    private val configKey = "$filterName:config"
    private val commands: RedisCommands<String, String> = connection.sync()

    /** 비트 배열 크기 m = ceil(-n·ln(p) / (ln2)²) */
    val m: Long = ceil(-options.expectedInsertions * ln(options.falseProbability) / ln(2.0).pow(2)).toLong()

    /** 해시 함수 수 k = round(m/n · ln2) */
    val k: Int = (m.toDouble() / options.expectedInsertions * ln(2.0)).roundToInt().coerceAtLeast(1)

    /**
     * 필터를 초기화합니다.
     *
     * 이미 초기화된 경우 파라미터가 다르면 [IllegalStateException]을 던집니다.
     * 동일 파라미터로 재호출하면 false를 반환합니다 (멱등성).
     */
    fun tryInit(): Boolean {
        val set = commands.hsetnx(configKey, "k", k.toString())
        if (set) {
            commands.hset(
                configKey, mapOf(
                    "m" to m.toString(),
                    "n" to options.expectedInsertions.toString(),
                    "p" to options.falseProbability.toString(),
                )
            )
            log.debug { "BloomFilter 초기화: name=$filterName, m=$m, k=$k" }
            return true
        }
        // 재초기화 시 파라미터 불일치 검증
        val storedM = commands.hget(configKey, "m")?.toLongOrNull()
        val storedK = commands.hget(configKey, "k")?.toIntOrNull()
        if (storedM != null && storedK != null && (storedM != m || storedK != k)) {
            throw IllegalStateException(
                "BloomFilter '$filterName' 이미 다른 파라미터로 초기화됨: " +
                    "저장된 m=$storedM/k=$storedK, 현재 m=$m/k=$k"
            )
        }
        return false
    }

    /**
     * 원소를 추가합니다. String 원소를 UTF-8로 인코딩하여 해시 위치를 계산합니다.
     */
    fun add(element: String) {
        val positions = hashPositions(element)
        commands.eval<Long>(ADD_SCRIPT, ScriptOutputType.INTEGER, arrayOf(filterName), *positions)
        log.debug { "BloomFilter add: name=$filterName, element=$element" }
    }

    /**
     * 원소가 필터에 포함되어 있는지 확인합니다.
     * false이면 확실히 없음. true이면 있거나 오탐.
     */
    fun contains(element: String): Boolean {
        val positions = hashPositions(element)
        return commands.eval<Long>(
            CONTAINS_SCRIPT, ScriptOutputType.INTEGER, arrayOf(filterName), *positions
        ) == 1L
    }

    override fun close() = connection.close()

    /**
     * MurmurHash3 128bit를 이용해 k개 비트 위치(문자열)를 반환합니다.
     * 공식: positions[i] = |h1 + i·h2| mod m
     */
    private fun hashPositions(element: String): Array<String> {
        val bytes = element.toByteArray(Charsets.UTF_8)
        val (h1, h2) = Murmur3.hash128x64(bytes)
        return Array(k) { i ->
            Math.floorMod(h1 + i.toLong() * h2, m).toString()
        }
    }
}
