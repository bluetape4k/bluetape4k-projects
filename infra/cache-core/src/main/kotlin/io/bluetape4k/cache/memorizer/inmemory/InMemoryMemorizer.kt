package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * InMemory 이용하여 [InMemoryMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @return [Memorizer] instance
 */
@Deprecated(
    message = "memorizer()는 memoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("memoizer()", "io.bluetape4k.cache.memoizer.inmemory.memoizer"),
    level = DeprecationLevel.WARNING
)
fun <T: Any, R: Any> ((T) -> R).memorizer(): InMemoryMemorizer<T, R> =
    InMemoryMemorizer(this)

/**
 * 함수의 실행 결과를 캐시하여, 재 호출 시 캐시된 내용을 제공하도록 합니다.
 *
 * @param evaluator    수행할 함수
 */
@Deprecated(
    message = "InMemoryMemorizer는 InMemoryMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("InMemoryMemoizer", "io.bluetape4k.cache.memoizer.inmemory.InMemoryMemoizer"),
    level = DeprecationLevel.WARNING
)
class InMemoryMemorizer<in T: Any, out R: Any>(
    @BuilderInference private val evaluator: (T) -> R,
): Memorizer<T, R> {

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
