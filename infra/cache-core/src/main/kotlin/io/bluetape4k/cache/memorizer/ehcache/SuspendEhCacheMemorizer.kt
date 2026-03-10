package io.bluetape4k.cache.memorizer.ehcache

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ehcache를 사용하는 suspend memorizer를 생성합니다.
 *
 * ## 동작/계약
 * - 키가 캐시에 있으면 evaluator를 실행하지 않고 캐시 값을 반환합니다.
 * - miss 시 [evaluator] 실행 결과를 캐시에 저장합니다.
 * - 저장/조회는 Ehcache 동기 API로 수행됩니다.
 *
 * ```kotlin
 * val memo = cache.suspendMemorizer<String, Int> { it.length }
 * // memo("abcd") == 4
 * ```
 */
@Deprecated(
    message = "suspendMemorizer()는 suspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("suspendMemoizer(evaluator)", "io.bluetape4k.cache.memoizer.ehcache.suspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> org.ehcache.Cache<T, R>.suspendMemorizer(
    @BuilderInference evaluator: suspend (T) -> R,
): SuspendEhCacheMemorizer<T, R> =
    SuspendEhCacheMemorizer(this, evaluator)

/**
 * suspend 함수를 Ehcache 기반 memorizer로 감쌉니다.
 *
 * ## 동작/계약
 * - [cache]를 저장소로 사용해 동일 입력의 결과를 재사용합니다.
 * - 캐시에 값이 없을 때만 원본 suspend 함수를 실행합니다.
 * - 새 [SuspendEhCacheMemorizer] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).withSuspendMemorizer(cache)
 * // memo("abc") == 3
 * ```
 */
@Deprecated(
    message = "withSuspendMemorizer()는 withSuspendMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("withSuspendMemoizer(cache)", "io.bluetape4k.cache.memoizer.ehcache.withSuspendMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> (suspend (T) -> R).withSuspendMemorizer(
    cache: org.ehcache.Cache<T, R>,
): SuspendEhCacheMemorizer<T, R> =
    SuspendEhCacheMemorizer(cache, this)

/**
 * Ehcache 기반 [SuspendMemorizer] 구현체입니다.
 *
 * ## 동작/계약
 * - [invoke]는 캐시 hit 시 즉시 반환하고 miss 시 evaluator 결과를 저장합니다.
 * - [clear]는 mutex 보호 하에 `cache.clear()`를 호출합니다.
 * - evaluator 예외는 캐시에 저장되지 않고 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val memo = SuspendEhCacheMemorizer(cache) { key: String -> key.length }
 * // memo("abcd") == 4
 * ```
 */
@Deprecated(
    message = "SuspendEhCacheMemorizer는 SuspendEhCacheMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("SuspendEhCacheMemoizer", "io.bluetape4k.cache.memoizer.ehcache.SuspendEhCacheMemoizer"),
    level = DeprecationLevel.WARNING
)
class SuspendEhCacheMemorizer<T: Any, R: Any>(
    private val cache: org.ehcache.Cache<T, R>,
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLogging()

    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return cache.get(input)
            ?: run {
                val result = evaluator(input)
                cache.put(input, result)
                result
            }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}
