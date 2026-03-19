package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.SuspendBloomFilter
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await

/**
 * Lettuce 를 사용하는 Coroutines 방식의 Bloom Filter
 *
 * ## 동작/계약
 * - 내부적으로 Lettuce 비동기 API 와 Lua Script 를 사용하여 원자적으로 비트를 설정/조회합니다.
 * - 상태는 Redis 에 저장되며, [bloomName] 키에 비트열로 관리됩니다.
 * - [bloomName]이 blank면 생성 시 [IllegalArgumentException]이 발생합니다.
 * - [contains] `true`는 오탐 가능성이 있고 `false`는 미포함이 확정입니다.
 *
 * ```
 * val connection = RedisClient.create("redis://localhost").connect()
 * val bloomFilter = LettuceSuspendBloomFilter<String>(connection, "bloom-filter")
 *
 * runBlocking {
 *     val items = List(100) { Fakers.fixedString(16) }.distinct()
 *     items.forEach { bloomFilter.add(it) }
 *
 *     // 기존 Item이 존재하는지 검증
 *     items.all { bloomFilter.contains(it) }.shouldBeTrue()
 *
 *     // 기존 Item이 아닌 값이 존재하지 않는지 검증
 *     bloomFilter.contains("not-exists").shouldBeFalse()
 * }
 * ```
 *
 * @property connection Lettuce [StatefulRedisConnection]
 * @property bloomName Bloom Filter 이름 (Redis 키)
 * @property m Bloom Filter 크기
 * @property k Hash 함수 개수
 */
class LettuceSuspendBloomFilter<T: Any> private constructor(
    private val connection: StatefulRedisConnection<String, String>,
    private val bloomName: String,
    override val m: Int,
    override val k: Int,
): SuspendBloomFilter<T> {

    companion object: KLoggingChannel() {

        /**
         * [LettuceSuspendBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [bloomName]이 blank면 [IllegalArgumentException]을 던집니다.
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산해 생성합니다.
         *
         * ```kotlin
         * val filter = LettuceSuspendBloomFilter<String>(connection, "user-email")
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
        ): LettuceSuspendBloomFilter<T> {
            bloomName.requireNotBlank("bloomName")

            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return LettuceSuspendBloomFilter<T>(connection, bloomName, m, k).apply {
                log.info { "Create LettuceSuspendBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    private val asyncCommands get() = connection.async()

    override val isEmpty: Boolean
        get() = connection.sync().exists(bloomName) == 0L

    override suspend fun add(value: T) {
        val offsets = getOffsets(value)
        asyncCommands.eval<Long>(
            LettuceBloomFilterScripts.ADD_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(bloomName),
            *offsets
        )
            .await()
    }

    /**
     * 원소 포함 여부 검사
     *
     * ## 동작/계약
     * - Lua Script 로 비동기 비트 조회 결과가 모두 `1`이면 `true`를 반환합니다.
     * - Redis I/O 예외는 suspend 호출자에게 전파됩니다.
     *
     * ```kotlin
     * bloomFilter.add("alpha")
     * // bloomFilter.contains("alpha") == true
     * ```
     */
    override suspend fun contains(value: T): Boolean {
        val offsets = getOffsets(value)
        val result = asyncCommands.eval<Long>(
            LettuceBloomFilterScripts.CONTAINS_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(bloomName),
            *offsets
        )
            .await()
        return result == 1L
    }

    override suspend fun count(): Long {
        return asyncCommands.bitcount(bloomName).await()
    }

    override suspend fun clear() {
        asyncCommands.del(bloomName).await()
    }

    private fun getOffsets(value: T): Array<String> =
        Hasher.murmurHashOffset(value, k, m).map { it.toString() }.toTypedArray()
}
