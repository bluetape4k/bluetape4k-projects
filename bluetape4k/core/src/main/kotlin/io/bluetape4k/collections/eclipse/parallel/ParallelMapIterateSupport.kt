package io.bluetape4k.collections.eclipse.parallel

import org.eclipse.collections.impl.parallel.ParallelMapIterate
import java.util.concurrent.ExecutorService


inline fun <K, V> Map<K, V>.parForEach(
    @BuilderInference crossinline prcedure: (K, V) -> Unit,
) {
    ParallelMapIterate.forEachKeyValue(this) { k: K, v: V -> prcedure(k, v) }
}

inline fun <K, V, R> Map<K, V>.parMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    @BuilderInference crossinline mapper: (K, V) -> R,
): Collection<R> =
    asIterable().parMap(batchSize, executor, reorder) { (k: K, v: V) -> mapper(k, v) }

inline fun <K, V, R> Map<K, V>.parFlatMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    @BuilderInference crossinline flatMapper: (K, V) -> Collection<R>,
): Collection<R> =
    asIterable().parFlatMap(batchSize, executor, reorder) { (k: K, v: V) -> flatMapper(k, v) }
