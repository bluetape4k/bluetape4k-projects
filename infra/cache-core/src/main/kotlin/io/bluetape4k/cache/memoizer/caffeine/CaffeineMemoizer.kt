package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Caffeine Cache를 이용하여 [CaffeineMemoizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.memoizer(
    @BuilderInference evaluator: (T) -> R,
): CaffeineMemoizer<T, R> =
    CaffeineMemoizer(this, evaluator)

fun <T: Any, R: Any> ((T) -> R).withMemoizer(cache: Cache<T, R>): CaffeineMemoizer<T, R> =
    CaffeineMemoizer(cache, this)

/**
 * Caffeine Cache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class CaffeineMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val lock = ReentrantLock()

    override fun invoke(input: T): R {
        return cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override fun clear() {
        lock.withLock {
            cache.cleanUp()
        }
    }
}
