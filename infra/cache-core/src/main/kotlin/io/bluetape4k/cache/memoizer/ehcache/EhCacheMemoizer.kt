package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Ehcache를 이용하여 [EhCacheMemoizer]를 생성합니다.
 */
fun <T : Any, R : Any> org.ehcache.Cache<T, R>.memoizer(
    evaluator: (T) -> R,
): EhCacheMemoizer<T, R> = EhCacheMemoizer(this, evaluator)

/**
 * 함수를 Ehcache 기반 [EhCacheMemoizer]로 감쌉니다.
 */
fun <T : Any, R : Any> ((T) -> R).withMemoizer(cache: org.ehcache.Cache<T, R>): EhCacheMemoizer<T, R> =
    EhCacheMemoizer(cache, this)

/**
 * Ehcache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 */
class EhCacheMemoizer<T : Any, R : Any>(
    private val cache: org.ehcache.Cache<T, R>,
    private val evaluator: (T) -> R,
) : Memoizer<T, R> {
    companion object : KLogging()

    private val lock = ReentrantLock()

    override fun invoke(key: T): R =
        cache.get(key)
            ?: run {
                val result = evaluator(key)
                cache.put(key, result)
                result
            }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
