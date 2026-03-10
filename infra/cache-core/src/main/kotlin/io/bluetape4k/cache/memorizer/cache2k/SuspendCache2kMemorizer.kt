package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache2k를 사용하는 suspend memorizer를 생성합니다.
 *
 * ## 동작/계약
 * - 키가 존재하면 evaluator를 실행하지 않고 캐시 값을 반환합니다.
 * - miss 시 evaluator 결과를 Cache2k에 저장한 뒤 반환합니다.
 * - 반환 타입은 [SuspendMemorizer] 인터페이스입니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemorizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
@Deprecated(
    message = "suspendMemorizer()는 suspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("suspendMemoizer(evaluator)", "io.bluetape4k.cache.memoizer.cache2k.suspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> org.cache2k.Cache<T, R>.suspendMemorizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> =
    SuspendCache2kMemorizer(this, evaluator)

/**
 * suspend 함수를 Cache2k 기반 memorizer로 감쌉니다.
 *
 * ## 동작/계약
 * - [cache]를 저장소로 사용해 동일 입력의 계산을 재사용합니다.
 * - 캐시에 값이 없을 때만 원본 suspend 함수를 실행합니다.
 * - 새 [SuspendCache2kMemorizer] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemorizer(cache)
 * // memo("abc") == 3
 * ```
 */
@Deprecated(
    message = "withSuspendMemorizer()는 withSuspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("withSuspendMemoizer(cache)", "io.bluetape4k.cache.memoizer.cache2k.withSuspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    cache: org.cache2k.Cache<T, R>,
): SuspendMemorizer<T, R> =
    SuspendCache2kMemorizer(cache, this)

/**
 * Cache2k 기반 [SuspendMemorizer] 구현체입니다.
 *
 * ## 동작/계약
 * - [invoke]는 캐시 hit 시 즉시 반환하고 miss 시 evaluator 결과를 저장합니다.
 * - [clear]는 mutex 보호 하에 `cache.clear()`를 수행합니다.
 * - evaluator 예외는 저장 없이 그대로 전파됩니다.
 *
 * ```kotlin
 * val memo = SuspendCache2kMemorizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
@Deprecated(
    message = "SuspendCache2kMemorizer는 SuspendCache2kMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("SuspendCache2kMemoizer", "io.bluetape4k.cache.memoizer.cache2k.SuspendCache2kMemoizer"),
    level = DeprecationLevel.WARNING
)
class SuspendCache2kMemorizer<in T: Any, out R: Any>(
    private val cache: org.cache2k.Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.get(input) ?: run {
            val result = evaluator(input)
            this@SuspendCache2kMemorizer.cache.put(input, result)
            result
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}
