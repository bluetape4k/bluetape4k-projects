package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caffeine [Cache]를 사용하는 suspend memoizer를 생성합니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemoizer<String, Int> { it.length }
 * val size = memo("abcd")
 * // size == 4
 * ```
 */
fun <T: Any, R: Any> Cache<T, R>.suspendMemoizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendCaffeineMemoizer<T, R> =
    SuspendCaffeineMemoizer(this, evaluator)

/**
 * suspend 함수를 Caffeine 캐시와 결합한 memoizer로 감쌉니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemoizer(cache)
 * // memo("abc") == 3
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemoizer(cache: Cache<T, R>): SuspendCaffeineMemoizer<T, R> =
    SuspendCaffeineMemoizer(cache, this)

/**
 * Caffeine 기반 [SuspendMemoizer] 구현체입니다.
 *
 * ```kotlin
 * val memo = SuspendCaffeineMemoizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
class SuspendCaffeineMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.getIfPresent(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.cleanUp()
        }
    }
}
