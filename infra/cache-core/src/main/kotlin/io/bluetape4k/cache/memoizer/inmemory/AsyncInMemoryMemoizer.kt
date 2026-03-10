package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * InMemory를 이용하여 [AsyncInMemoryMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).asyncMemoizer(): AsyncInMemoryMemoizer<T, R> =
    AsyncInMemoryMemoizer(this)

/**
 * 로컬 메모리에 [evaluator] 실행 결과를 저장합니다.
 */
class AsyncInMemoryMemoizer<in T: Any, R: Any>(
    @BuilderInference private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<T, R>()
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
