package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * InMemory를 이용하여 [InMemoryMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> key.length }).memoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // 캐시에서 즉시 반환
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> R).memoizer(): InMemoryMemoizer<T, R> =
    InMemoryMemoizer(this)

/**
 * 함수의 실행 결과를 캐시하여, 재 호출 시 캐시된 내용을 제공하도록 합니다.
 *
 * ```kotlin
 * val memo = InMemoryMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 *
 * @param evaluator 수행할 함수
 */
class InMemoryMemoizer<in T: Any, out R: Any>(
    private val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val resultCache = ConcurrentHashMap<T, R>()
    private val lock = ReentrantLock()

    override fun invoke(input: T): R {
        return resultCache.getOrPut(input) { evaluator(input) }
    }

    override fun clear() {
        lock.withLock {
            resultCache.clear()
        }
    }
}
