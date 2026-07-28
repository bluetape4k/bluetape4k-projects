package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.cache.memoizer.SingleFlight
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * 이 blocking evaluator를 위한 [InMemoryMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).memoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // returned immediately from the cache
 * // result2 == 5, returned from the cache
 * ```
 */
fun <T: Any, R: Any> ((T) -> R).memoizer(): InMemoryMemoizer<T, R> =
    InMemoryMemoizer(this)

/**
 * 성공한 evaluator 결과를 local [ConcurrentHashMap]에 저장하는 in-memory [Memoizer]입니다.
 *
 * 같은 key의 동시 cache miss는 [SingleFlight]를 통해 하나의 in-flight evaluator를 공유합니다.
 * [clear]는 cached value와 in-flight write token을 모두 무효화합니다. [clear] 전에 evaluator가 시작된
 * 호출자는 계산된 값을 그대로 받지만, 그 stale value는 cache에 다시 쓰지 않습니다.
 *
 * ```kotlin
 * val memo = InMemoryMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 *
 * @param T cache key와 evaluator input 타입입니다.
 * @param R cache value와 evaluator result 타입입니다.
 * @property evaluator cache miss일 때 값을 계산하는 blocking 함수입니다.
 */
class InMemoryMemoizer<in T: Any, out R: Any>(
    private val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val resultCache = ConcurrentHashMap<T, R>()
    private val singleFlight = SingleFlight<@UnsafeVariance T, @UnsafeVariance R>()

    override fun invoke(input: T): R {
        val cache = resultCache
        val flights = singleFlight

        cache[input]?.let { return it }

        return flights.run(input) { token ->
            cache[input]?.let { cached ->
                cached
            } ?: evaluator(input).also { result ->
                if (flights.isCurrent(token)) {
                    cache[input] = result
                }
            }
        }
    }

    override fun clear() {
        singleFlight.clear()
        resultCache.clear()
    }
}
