package io.bluetape4k.cache.memorizer.ehcache

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Ehcache 이용하여 [EhCacheMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
@Deprecated(
    message = "memorizer()는 memoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("memoizer(evaluator)", "io.bluetape4k.cache.memoizer.ehcache.memoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> org.ehcache.Cache<T, R>.memorizer(
    @BuilderInference evaluator: (T) -> R,
): EhCacheMemorizer<T, R> =
    EhCacheMemorizer(this, evaluator)

@Deprecated(
    message = "withMemorizer()는 withMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("withMemoizer(cache)", "io.bluetape4k.cache.memoizer.ehcache.withMemoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> ((T) -> R).withMemorizer(cache: org.ehcache.Cache<T, R>): EhCacheMemorizer<T, R> =
    EhCacheMemorizer(cache, this)

/**
 * Ehcache 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
@Deprecated(
    message = "EhCacheMemorizer는 EhCacheMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("EhCacheMemoizer", "io.bluetape4k.cache.memoizer.ehcache.EhCacheMemoizer"),
    level = DeprecationLevel.WARNING
)
class EhCacheMemorizer<T: Any, R: Any>(
    private val cache: org.ehcache.Cache<T, R>,
    @BuilderInference private val evaluator: (T) -> R,
): Memorizer<T, R> {

    companion object: KLogging()

    private val lock = ReentrantLock()

    override fun invoke(key: T): R {
        return cache.get(key)
            ?: run {
                val result = evaluator(key)
                cache.put(key, result)
                result
            }
    }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
