package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.memorizer.Memorizer
import kotlinx.atomicfu.locks.ReentrantLock
import org.cache2k.Cache
import kotlin.concurrent.withLock

/**
 * Cache2k Cache를 이용하여 [Memorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.memorizer(evaluator: (T) -> R): Memorizer<T, R> =
    Cache2kMemorizer(this, evaluator)

/**
 * Cache2k Cache를 이용하여 [Memorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param cache 함수 실행 결과를 캐싱하는 캐시
 */
fun <T: Any, R: Any> ((T) -> R).withMemorizer(cache: Cache<T, R>): Memorizer<T, R> =
    Cache2kMemorizer(cache, this)

/**
 * Cache2k Cache를 이용하여 메소드의 실행 결과를 기억하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 *

 */
class Cache2kMemorizer<in T: Any, out R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> R,
): Memorizer<T, R> {

    private val lock = ReentrantLock()

    override fun invoke(input: T): R {
        return cache.computeIfAbsent(input) { evaluator(input) }
    }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
