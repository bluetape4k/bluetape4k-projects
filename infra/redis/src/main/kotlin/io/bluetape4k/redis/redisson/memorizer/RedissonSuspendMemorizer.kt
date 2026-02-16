package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.redisson.api.RMap

/**
 * [evaluator]가 실행한 결과를 Redis 에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = map.memorizer { key -> key * key }
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @param T  key type
 * @param R  value type
 * @receiver Redisson [RMap] 인스턴스
 * @param evaluator 실행할 함수
 * @return [RedissonMemorizer] instance
 */
fun <T: Any, R: Any> RMap<T, R>.suspendMemorizer(evaluator: suspend (T) -> R): RedissonSuspendMemorizer<T, R> =
    RedissonSuspendMemorizer(this, evaluator)

/**
 * 함수의 실행 결과를 Redisson `RMap` 에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```
 * val map: RMap<String, Int> = redisson.getMap("map")
 * val memorizer = { key: Int -> key * key }.memorizer(map)
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @param T key type
 * @param R value type
 * @receiver 실행할 함수
 * @param map Redisson [RMap] 인스턴스
 * @return [RedissonMemorizer] instance
 */
fun <T: Any, R: Any> (suspend (T) -> R).memorizer(map: RMap<T, R>): RedissonSuspendMemorizer<T, R> =
    RedissonSuspendMemorizer(map, this)

/**
 * [evaluator]가 실행한 결과를 [map]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = RedissonMemorizer(map) { key -> key * key }
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @property map       Redisson [RMap] 인스턴스
 * @property evaluator 실행할 함수
 */
class RedissonSuspendMemorizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLogging()

    override suspend fun invoke(key: T): R {
        return map.getOrPut(key) { evaluator(key) }
    }

    override suspend fun clear() {
        log.debug { "Clear all memorized values. map=${map.name}" }
        map.clearAsync().awaitSuspending()
    }
}
