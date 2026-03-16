package io.bluetape4k.collections.eclipse.parallel

import io.bluetape4k.collections.eclipse.fastListOf
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

/**
 * DEFAULT_PARALLEL_BATCH_SIZE 값을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val value = DEFAULT_PARALLEL_BATCH_SIZE
 * // value
 * // true
 * ```
 */
const val DEFAULT_PARALLEL_BATCH_SIZE = 10_000

/**
 * DEFAULT_REORDER 값을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val value = DEFAULT_REORDER
 * // value
 * // true
 * ```
 */
const val DEFAULT_REORDER = false

/**
 * AVAILABLE_PROCESSORS 값을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val value = AVAILABLE_PROCESSORS
 * // value
 * // true
 * ```
 */
val AVAILABLE_PROCESSORS: Int = Runtime.getRuntime().availableProcessors()

/**
 * PARALLEL_EXECUTOR_SERVICE 값을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val value = PARALLEL_EXECUTOR_SERVICE
 * // value
 * // true
 * ```
 */
val PARALLEL_EXECUTOR_SERVICE: ExecutorService = ForkJoinPool.commonPool()

/**
 * parFilter 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3, 4).parFilter { it % 2 == 0 }
 * // result contains [2, 4]
 * ```
 */
inline fun <T> Iterable<T>.parFilter(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
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

/**
 * parFilterNot 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3, 4).parFilterNot { it % 2 == 0 }
 * // result contains [1, 3]
 * ```
 */
inline fun <T> Iterable<T>.parFilterNot(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
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

/**
 * parCount 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3, 4).parCount { it > 2 }
 * // result == 2
 * ```
 */
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

/**
 * parForEach 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val values = listOf(1, 2, 3)
 * values.parForEach { /* 병렬 처리 */ }
 * // 모든 요소에 대해 action이 실행됨
 * ```
 */
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

/**
 * parForEachWithIndex 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val input = listOf("a", "b")
 * input.parForEachWithIndex { index, value -> /* index/value 사용 */ }
 * // (0,"a"), (1,"b")가 전달됨
 * ```
 */
inline fun <T> Iterable<T>.parForEachWithIndex(
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    crossinline action: (index: Int, element: T) -> Unit,
) {
    ParallelIterate.forEachWithIndex(
        this,
        ObjectIntProcedure { value: T, index: Int -> action(index, value) },
        executor
    )
}

/**
 * parForEachWithIndex 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val input = listOf("a", "b")
 * input.parForEachWithIndex { index, value -> /* index/value 사용 */ }
 * // (0,"a"), (1,"b")가 전달됨
 * ```
 */
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

/**
 * parMap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parMap { it * 2 }
 * // result contains [2, 4, 6]
 * ```
 */
inline fun <T, R> Iterable<T>.parMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
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

/**
 * parFlatMap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2).parFlatMap { listOf(it, -it) }
 * // result contains [1, -1, 2, -2]
 * ```
 */
inline fun <T, R> Iterable<T>.parFlatMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
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

/**
 * parFilterMap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parFilterMap({ it % 2 == 1 }) { it * 10 }
 * // result contains [10, 30]
 * ```
 */
inline fun <T, R> Iterable<T>.parFilterMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    crossinline predicate: (T) -> Boolean,
    crossinline mapper: (T) -> R,
): Collection<R> =
    ParallelIterate.collectIf(
        this,
        Predicate { predicate(it) },
        { mapper(it) },
        fastListOf(),
        batchSize,
        executor,
        reorder
    )

/**
 * parGroupBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf("a", "bb").parGroupBy { it.length }
 * // result[1] contains ["a"], result[2] contains ["bb"]
 * ```
 */
inline fun <K, V> Iterable<V>.parGroupBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    crossinline keyFunction: (V) -> K,
): MutableMultimap<K, V> =
    ParallelIterate.groupBy(
        this,
        { keyFunction(it) },
        concurrentMultimap,
        batchSize,
        executor
    )


/**
 * parAggregateBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parAggregateBy({ it % 2 }, { 0 }) { acc, v -> acc + v }
 * // result[1] == 4, result[0] == 2
 * ```
 */
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

/**
 * parAggregateInPlaceBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parAggregateInPlaceBy({ it % 2 }, { mutableListOf<Int>() }) { acc, v -> acc += v }
 * // result[1] contains [1, 3]
 * ```
 */
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
/**
 * parSumBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parSumBy { it.toLong() }
 * // result == 6
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Double,
): ObjectDoubleMap<V> =
    ParallelIterate.sumByDouble(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByFloat")
/**
 * parSumBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parSumBy { it.toLong() }
 * // result == 6
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Float,
): ObjectDoubleMap<V> =
    ParallelIterate.sumByFloat(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByLong")
/**
 * parSumBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parSumBy { it.toLong() }
 * // result == 6
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Long,
): ObjectLongMap<V> =
    ParallelIterate.sumByLong(this, { groupBy(it) }, { func(it) })

@JvmName("parSumByInt")
/**
 * parSumBy 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 3).parSumBy { it.toLong() }
 * // result == 6
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    crossinline groupBy: (T) -> V,
    crossinline func: (T) -> Int,
): ObjectLongMap<V> =
    ParallelIterate.sumByInt(this, { groupBy(it) }, { func(it) })
