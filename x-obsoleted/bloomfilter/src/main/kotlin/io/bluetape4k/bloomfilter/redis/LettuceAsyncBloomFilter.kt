package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.bloomfilter.DEFAULT_MAX_NUM
import io.bluetape4k.bloomfilter.Hasher
import io.bluetape4k.bloomfilter.optimalK
import io.bluetape4k.bloomfilter.optimalM
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce 를 사용하는 비동기(Async) Bloom Filter
 *
 * ## 동작/계약
 * - 모든 연산이 [RedisFuture]를 반환하는 비동기 API입니다.
 * - 내부적으로 Lettuce 비동기 명령과 Lua Script 를 사용하여
 *   원자적으로 비트를 설정/조회합니다.
 * - 상태는 Redis 에 저장되며, [bloomName] 키에 비트열로 관리됩니다.
 * - [containsAsync] 결과가 `1L`이면 포함 가능(오탐 가능), `0L`이면 미포함 확정입니다.
 * - 해시 오프셋은 [Hasher.murmurHashOffset]으로 계산합니다.
 *
 * ```
 * val connection = RedisClient.create("redis://localhost").connect()
 * val bloomFilter = LettuceAsyncBloomFilter<String>(connection, "bloom-filter")
 *
 * // 비동기 추가
 * bloomFilter.addAsync("item1").get()
 *
 * // 비동기 포함 여부 검사
 * val future: RedisFuture<Long> = bloomFilter.containsAsync("item1")
 * val exists = future.get() == 1L  // true
 * ```
 *
 * @property connection Lettuce [StatefulRedisConnection]
 * @property bloomName Bloom Filter 이름 (Redis 키)
 * @property m Bloom Filter 크기
 * @property k Hash 함수 개수
 */
class LettuceAsyncBloomFilter<T: Any> private constructor(
    private val connection: StatefulRedisConnection<String, String>,
    private val bloomName: String,
    val m: Int,
    val k: Int,
) {

    companion object: KLogging() {

        /**
         * [LettuceAsyncBloomFilter] 를 생성합니다.
         *
         * ## 동작/계약
         * - [bloomName]이 blank면 [IllegalArgumentException]을 던집니다.
         * - [maxNum], [errorRate]로 최적 [m], [k]를 계산해 생성합니다.
         *
         * ```kotlin
         * val filter = LettuceAsyncBloomFilter<String>(connection, "user-email")
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
        ): LettuceAsyncBloomFilter<T> {
            bloomName.requireNotBlank("bloomName")

            val m = optimalM(maxNum, errorRate)
            val k = optimalK(maxNum, m)

            return LettuceAsyncBloomFilter<T>(connection, bloomName, m, k).apply {
                log.info { "Create LettuceAsyncBloomFilter, name=$bloomName, m=$m, k=$k" }
            }
        }
    }

    private val asyncCommands get() = connection.async()

    /**
     * 필터에 어떤 원소도 추가되지 않은 초기 상태인지 여부입니다.
     *
     * 키 존재 여부를 동기적으로 확인합니다.
     */
    val isEmpty: Boolean
        get() = connection.sync().exists(bloomName) == 0L

    /**
     * 원소를 비동기로 추가합니다.
     *
     * ## 동작/계약
     * - Lua Script 로 모든 해시 오프셋 비트를 원자적으로 설정합니다.
     * - 반환되는 [RedisFuture]가 완료되면 비트 설정이 확정됩니다.
     *
     * @param value 추가할 원소
     * @return 완료 시 `1L`을 반환하는 [RedisFuture]
     */
    fun addAsync(value: T): RedisFuture<Long> {
        val offsets = getOffsets(value)
        return asyncCommands.eval(LettuceBloomFilterScripts.ADD_SCRIPT, ScriptOutputType.INTEGER, arrayOf(bloomName), *offsets)
    }

    /**
     * 원소 포함 여부를 비동기로 검사합니다.
     *
     * ## 동작/계약
     * - Lua Script 로 모든 해시 오프셋 비트를 원자적으로 조회합니다.
     * - 결과가 `1L`이면 포함 가능(오탐 가능), `0L`이면 미포함 확정입니다.
     *
     * @param value 검사할 원소
     * @return `1L`(포함 가능) 또는 `0L`(미포함 확정)을 반환하는 [RedisFuture]
     */
    fun containsAsync(value: T): RedisFuture<Long> {
        val offsets = getOffsets(value)
        return asyncCommands.eval(LettuceBloomFilterScripts.CONTAINS_SCRIPT, ScriptOutputType.INTEGER, arrayOf(bloomName), *offsets)
    }

    /**
     * 설정된 비트 수를 비동기로 조회합니다.
     *
     * @return 설정된 비트 수를 반환하는 [RedisFuture]
     */
    fun countAsync(): RedisFuture<Long> {
        return asyncCommands.bitcount(bloomName)
    }

    /**
     * 필터 상태를 비동기로 초기화합니다.
     *
     * @return 삭제된 키 수를 반환하는 [RedisFuture]
     */
    fun clearAsync(): RedisFuture<Long> {
        return asyncCommands.del(bloomName)
    }

    private fun getOffsets(value: T): Array<String> =
        Hasher.murmurHashOffset(value, k, m).map { it.toString() }.toTypedArray()
}
