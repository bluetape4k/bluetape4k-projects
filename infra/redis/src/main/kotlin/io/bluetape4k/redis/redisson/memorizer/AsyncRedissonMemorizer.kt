package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.concurrent.flatMap
import io.bluetape4k.concurrent.map
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.redisson.api.RMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * 비동기로 실행되는 [evaluator]의 결과를 Redis에 저장하도록 합니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = map.asyncMemorizer { key -> futureOf { key * key } }
 * val result1 = memorizer(4).get() // 16
 * val result2 = memorizer(4).get() // 16  // memorized value
 * ```
 *
 * @param evaluator 수행할 비동기 함수
 * @return [AsyncRedissonMemorizer] 인스턴스
 */
fun <T: Any, R: Any> RMap<T, R>.asyncMemorizer(evaluator: (T) -> CompletionStage<R>): AsyncRedissonMemorizer<T, R> =
    AsyncRedissonMemorizer(this, evaluator)

/**
 * 비동기 함수의 실행 결과를 Redis의 `map`에 저장하도록 합니다.
 *
 * ```
 * val map: RMap<String, Int> = redisson.getMap("map")
 * val memorizer = { key: Int -> futureOf { key * key } }.asyncMemorizer(map)
 * val result1 = memorizer(4).get() // 16
 * val result2 = memorizer(4).get() // 16  // memorized value
 * ```
 *
 * @receiver 실행할 비동기 함수
 * @property map 수행결과를 저장할 Redisson [RMap] 인스턴스
 * @return [AsyncRedissonMemorizer] 인스턴스
 */
fun <T: Any, R: Any> ((T) -> CompletionStage<R>).asyncMemorizer(map: RMap<T, R>): AsyncRedissonMemorizer<T, R> =
    AsyncRedissonMemorizer(map, this)

/**
 * 비동기 [evaluator] 결과를 Redis에 저장하도록 합니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = AsyncRedissonMemorizer(map) { key -> futureOf { key * key } }
 * val result1 = memorizer(4).get() // 16
 * val result2 = memorizer(4).get() // 16  // memorized value
 * ```
 *
 * @property map 수행결과를 저장할 Redisson [RMap] 인스턴스
 * @property evaluator 수행할 비동기 함수
 */
class AsyncRedissonMemorizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: (T) -> CompletionStage<R>,
): AsyncMemorizer<T, R> {

    companion object: KLoggingChannel()

    override fun invoke(key: T): CompletableFuture<R> {
        return map
            .containsKeyAsync(key)
            .flatMap { exist ->
                if (exist) {
                    map.getAsync(key)
                } else {
                    evaluator(key).map { value -> value.apply { map.fastPutIfAbsentAsync(key, value) } }
                }
            }
            .toCompletableFuture()
    }

    override fun clear() {
        log.debug { "Clear all memorized values. map=${map.name}" }
        map.clear()
    }
}
