package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging

/**
 * Caffeine Cache를 이용하여 [CaffeineMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = cache.memoizer { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.memoizer(
    evaluator: (T) -> R,
): CaffeineMemoizer<T, R> = CaffeineMemoizer(this, evaluator)

/**
 * 함수를 Caffeine Cache 기반 [CaffeineMemoizer]로 감쌉니다.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = ({ key: String -> key.length }).withMemoizer(cache)
 * val result = memo("hello")
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> R).withMemoizer(cache: Cache<T, R>): CaffeineMemoizer<T, R> =
    CaffeineMemoizer(cache, this)

/**
 * Memoizes a synchronous function using a Caffeine [Cache] so repeated calls return the cached result.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = CaffeineMemoizer(cache) { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 *
 * ## Thread Safety
 * Uses a non-atomic read-evaluate-write pattern to support recursive evaluators (e.g., factorial,
 * fibonacci). `Cache.get(key, function)` would cause `IllegalStateException: Recursive update`
 * when the evaluator itself calls the memoizer for a different key on the same Caffeine cache.
 *
 * @property cache Caffeine cache that stores computed values
 * @property evaluator function that computes the value for a given input
 */
class CaffeineMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> R,
): Memoizer<T, R> {
    companion object: KLogging()

    override fun invoke(input: T): R =
        cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }

    override fun clear() {
        cache.invalidateAll()
    }
}
