package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.withLock

/**
 * InMemory 이용하여 [AsyncInMemoryMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).asyncMemorizer(): AsyncInMemoryMemorizer<T, R> =
    AsyncInMemoryMemorizer(this)


/**
 * 로컬 메모리에 [evaluator] 실행 결과를 저장합니다.
 *
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class AsyncInMemoryMemorizer<in T, R>(
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemorizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache: MutableMap<T, R> = ConcurrentHashMap<T, R>()
    private val lock = ReentrantLock()

    override fun invoke(input: T): CompletableFuture<R> {
        val promise = CompletableFuture<R>()

        try {
            if (resultCache.containsKey(input)) {
                promise.complete(resultCache[input])
            } else {
                evaluator(input)
                    .whenComplete { result, error ->
                        if (error != null) {
                            promise.completeExceptionally(error)
                        } else {
                            resultCache[input] = result
                            promise.complete(resultCache[input])
                        }
                    }
            }
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
        }

        return promise
    }

    override fun clear() {
        lock.withLock {
            resultCache.clear()
        }
    }
}
