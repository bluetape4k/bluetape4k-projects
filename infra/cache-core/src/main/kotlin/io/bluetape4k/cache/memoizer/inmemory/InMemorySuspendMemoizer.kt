package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * InMemory를 이용하여 [InMemorySuspendMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = (suspend { key: String -> key.length }).suspendMemoizer()
 * val result = memo("hello")
 * // result == 5
 * val result2 = memo("hello")  // 캐시에서 즉시 반환
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> (suspend (T) -> R).suspendMemoizer(): InMemorySuspendMemoizer<T, R> =
    InMemorySuspendMemoizer(this)

/**
 * 로컬 메모리에 suspend evaluator 실행 결과를 저장합니다.
 *
 * ```kotlin
 * val memo = InMemorySuspendMemoizer<String, Int> { key -> key.length }
 * val result = memo("hello")
 * // result == 5
 * ```
 */
class InMemorySuspendMemoizer<in T: Any, out R: Any>(
    private val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<T, R>()
    private val mutex = Mutex()

    override suspend fun invoke(input: T): R {
        return resultCache.getOrPut(input) {
            log.trace { "Cache miss for key: $input, evaluating..." }
            evaluator(input)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            resultCache.clear()
            log.trace { "Cleared in-memory cache." }
        }
    }
}
