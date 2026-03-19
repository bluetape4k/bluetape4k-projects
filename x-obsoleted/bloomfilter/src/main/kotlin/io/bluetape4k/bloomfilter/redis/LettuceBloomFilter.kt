package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.BloomFilter
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce 를 사용하는 Bloom Filter
 *
 * ## 동작/계약
 * - 내부적으로 Lettuce 의 `SETBIT`/`GETBIT` 명령을 Lua Script 로 배치 실행하여
 *   원자적으로 비트를 설정/조회합니다.
 * - 상태는 Redis 에 저장되며, [bloomName] 키에 비트열로 관리됩니다.
 * - [contains] `true`는 오탐 가능성이 있고 `false`는 미포함이 확정입니다.
 * - 해시 오프셋은 [Hasher.murmurHashOffset]으로 계산합니다.
 *
 * ```
 * val connection = RedisClient.create("redis://localhost").connect()
 * val bloomFilter = LettuceBloomFilter<String>(connection, "bloom-filter")
 *
 * val items = List(100) { Fakers.fixedString(16) }.distinct()
 * items.forEach { bloomFilter.add(it) }
 *
 * // 기존 Item이 존재하는지 검증
 * items.all { bloomFilter.contains(it) }.shouldBeTrue()
 *
 * // 기존 Item이 아닌 값이 존재하지 않는지 검증
 * bloomFilter.contains("not-exists").shouldBeFalse()
 * ```
 *
 * @property connection Lettuce [StatefulRedisConnection]
 * @property bloomName Bloom Filter 이름 (Redis 키)
 * @property m Bloom Filter 크기
 * @property k Hash 함수 개수
 */
class LettuceBloomFilter<T: Any> private constructor(
    private val connection: StatefulRedisConnection<String, String>,
    private val bloomName: String,
    override val m: Int,
    override val k: Int,
): BloomFilter<T> {

    companion object: KLogging() {

        /**
         * [LettuceBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [bloomName]이 blank면 [IllegalArgumentException]을 던집니다.
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산해 생성합니다.
         *
         * ```kotlin
         * val filter = LettuceBloomFilter<String>(connection, "user-email")
         * // filter.m > 0 && filter.k > 0
         * ```
         *
         * @param connection Lettuce [StatefulRedisConnection]
         * @param bloomName Bloom Filter 이름 (Redis 키)
         * @param maxNum 최대 요소 개수 (기본값: [DEFAULT_MAX_NUM])
         * @param errorRate 오류율 (기본값: [DEFAULT_ERROR_RATE])
         */
        @JvmStatic
        operator fun <T: Any> invoke(
            connection: StatefulRedisConnection<String, String>,
            bloomName: String,
            maxNum: Long = DEFAULT_MAX_NUM,
            errorRate: Double = DEFAULT_ERROR_RATE,
        ): LettuceBloomFilter<T> {
            bloomName.requireNotBlank("bloomName")

            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return LettuceBloomFilter<T>(connection, bloomName, m, k).apply {
                log.info { "Create LettuceBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    private val commands get() = connection.sync()

    override val isEmpty: Boolean
        get() = commands.exists(bloomName) == 0L

    override fun add(value: T) {
        val offsets = getOffsets(value)
        commands.eval<Long>(LettuceBloomFilterScripts.ADD_SCRIPT, ScriptOutputType.INTEGER, arrayOf(bloomName), *offsets)
    }

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - Lua Script 로 Redis BitSet 에서 오프셋 비트를 원자적으로 읽어
     *   모두 `1`이면 `true`를 반환합니다.
     * - Redis I/O 예외는 호출자에게 전파됩니다.
     *
     * ```kotlin
     * bloomFilter.add("alpha")
     * // bloomFilter.contains("alpha") == true
     * ```
     */
    override fun contains(value: T): Boolean {
        val offsets = getOffsets(value)
        val result = commands.eval<Long>(LettuceBloomFilterScripts.CONTAINS_SCRIPT, ScriptOutputType.INTEGER, arrayOf(bloomName), *offsets)
        return result == 1L
    }

    override fun count(): Long {
        return commands.bitcount(bloomName)
    }

    override fun clear() {
        commands.del(bloomName)
    }

    private fun getOffsets(value: T): Array<String> =
        Hasher.murmurHashOffset(value, k, m).map { it.toString() }.toTypedArray()
}
