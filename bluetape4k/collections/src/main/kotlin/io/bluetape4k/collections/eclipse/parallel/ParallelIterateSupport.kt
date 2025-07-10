package io.bluetape4k.collections.eclipse.parallel

import io.bluetape4k.collections.fastListOf
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.block.procedure.Procedure
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure
import org.eclipse.collections.api.map.primitive.ObjectDoubleMap
import org.eclipse.collections.api.map.primitive.ObjectLongMap
import org.eclipse.collections.api.multimap.MutableMultimap
import org.eclipse.collections.impl.multimap.list.SynchronizedPutFastListMultimap
import org.eclipse.collections.impl.parallel.ParallelIterate
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

const val DEFAULT_PARALLEL_BATCH_SIZE = 10_000
val AVAILABLE_PROCESSORS: Int = Runtime.getRuntime().availableProcessors()
val PARALLEL_EXECUTOR_SERVICE: ExecutorService = ForkJoinPool.commonPool()

inline fun <T> Iterable<T>.parFilter(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    crossinline predicate: (T) -> Boolean,
): Collection<T> =
    ParallelIterate.select(
        this,
        Predicate { predicate(it) },
        fastListOf(),
        batchSize,
        executor,
        reorder
    )

inline fun <T> Iterable<T>.parFilterNot(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    crossinline predicate: (T) -> Boolean,
): Collection<T> =
    ParallelIterate.reject(
        this,
        Predicate { predicate(it) },
        fastListOf(),
        batchSize,
        executor,
        reorder
    )

inline fun <T> Iterable<T>.parCount(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    crossinline predicate: (T) -> Boolean,
): Int =
    ParallelIterate.count(
        this,
        Predicate { predicate(it) },
        batchSize,
        executor,
    )

inline fun <T> Iterable<T>.parForEach(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    crossinline action: (T) -> Unit,
) {
    ParallelIterate.forEach(
        this,
        Procedure { action(it) },
        batchSize,
        executor,
    )
}

inline fun <T> Iterable<T>.parForEachWithIndex(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    crossinline action: (index: Int, element: T) -> Unit,
) {
    ParallelIterate.forEachWithIndex(
        this,
        ObjectIntProcedure { value: T, index: Int -> action(index, value) },
        executor
    )
}

inline fun <T> Iterable<T>.parForEachWithIndex(
    minForSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    taskCount: Int = AVAILABLE_PROCESSORS,
    crossinline action: (index: Int, element: T) -> Unit,
) {
    ParallelIterate.forEachWithIndex(
        this,
        ObjectIntProcedure { value: T, index: Int -> action(index, value) },
        minForSize,
        taskCount
    )
}

inline fun <T, R> Iterable<T>.parMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    crossinline mapper: (T) -> R,
): Collection<R> =
    ParallelIterate.collect(
        this,
        { mapper(it) },
        fastListOf(),
        batchSize,
        executor,
        reorder
    )

inline fun <T, R> Iterable<T>.parFlatMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    crossinline mapper: (element: T) -> Collection<R>,
): Collection<R> =
    ParallelIterate.flatCollect(
        this,
        { mapper(it) },
        fastListOf(),
        batchSize,
        executor,
        reorder
    )

inline fun <K, V, R: MutableMultimap<K, V>> Iterable<V>.parGroupBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    crossinline keyFunction: (V) -> K,
    multimapFactory: () -> R,
): MutableMultimap<K, V> =
    ParallelIterate.groupBy(
        this,
        { keyFunction(it) },
        concurrentMultimap,
        batchSize,
        executor
    )


inline fun <T, K, V> Iterable<T>.parAggregateBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    crossinline groupBy: (T) -> K,
    crossinline zeroValueFactory: () -> V,
    crossinline nonMutatingAggregator: (V, T) -> V,
): MutableMap<K, V> =
    ParallelIterate.aggregateBy(
        this,
        { groupBy(it) },
        { zeroValueFactory() },
        { ecc: V, item: T -> nonMutatingAggregator(ecc, item) },
        batchSize,
        executor
    )

inline fun <T, K, V> Iterable<T>.parAggregateInPlaceBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    crossinline groupBy: (T) -> K,
    crossinline zeroValueFactory: () -> V,
    crossinline mutatingAggregator: (V, T) -> Unit,
): MutableMap<K, V> =
    ParallelIterate.aggregateInPlaceBy(
        this,
        { groupBy(it) },
        { zeroValueFactory() },
        { ecc: V, item: T -> mutatingAggregator(ecc, item) },
        batchSize,
        executor
    )

@JvmName("parSumByDouble")
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Double,
): ObjectDoubleMap<V> =
    ParallelIterate.sumByDouble(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByFloat")
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Float,
): ObjectDoubleMap<V> =
    ParallelIterate.sumByFloat(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByLong")
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Long,
): ObjectLongMap<V> =
    ParallelIterate.sumByLong(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByInt")
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Int,
): ObjectLongMap<V> =
    ParallelIterate.sumByInt(this, { groupBy(it) }, { func(it) })
