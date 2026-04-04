package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
fun <T : Any, R : Any> Cache<T, R>.memoizer(
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
fun <T : Any, R : Any> ((T) -> R).withMemoizer(cache: Cache<T, R>): CaffeineMemoizer<T, R> =
    CaffeineMemoizer(cache, this)

/**
 * Caffeine Cache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = CaffeineMemoizer(cache) { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class CaffeineMemoizer<T : Any, R : Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> R,
) : Memoizer<T, R> {
    companion object : KLogging()

    private val lock = ReentrantLock()

    override fun invoke(input: T): R =
        cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }

    override fun clear() {
        lock.withLock {
            cache.cleanUp()
        }
    }
}
