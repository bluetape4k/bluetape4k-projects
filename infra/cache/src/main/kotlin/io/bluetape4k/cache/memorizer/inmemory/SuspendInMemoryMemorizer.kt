package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendInMemoryMemorizer<in T, out R>(
    @BuilderInference private val evaluator: suspend (T) -> R,
): SuspendMemorizer<T, R> {

    companion object: KLogging()

    private val resultCache: MutableMap<T, R> = mutableMapOf()
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
