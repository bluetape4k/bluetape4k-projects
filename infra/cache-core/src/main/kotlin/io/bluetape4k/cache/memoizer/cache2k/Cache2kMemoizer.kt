package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.Memoizer
import org.cache2k.Cache
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Cache2k Cache를 이용하여 [Memoizer]를 생성합니다.
 */
fun <T : Any, R : Any> Cache<T, R>.memoizer(
    evaluator: (T) -> R,
): Memoizer<T, R> = Cache2kMemoizer(this, evaluator)

/**
 * Cache2k Cache를 이용하여 [Memoizer]를 생성합니다.
 */
fun <T : Any, R : Any> ((T) -> R).withMemoizer(cache: Cache<T, R>): Memoizer<T, R> = Cache2kMemoizer(cache, this)

/**
 * Cache2k Cache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 */
class Cache2kMemoizer<in T : Any, out R : Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> R,
) : Memoizer<T, R> {
    private val lock = ReentrantLock()

    override fun invoke(input: T): R = cache.computeIfAbsent(input) { evaluator(input) }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
