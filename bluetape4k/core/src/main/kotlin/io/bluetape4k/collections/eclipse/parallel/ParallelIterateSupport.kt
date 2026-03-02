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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * val ref = ::parFilter
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parFilter(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    @BuilderInference crossinline predicate: (T) -> Boolean,
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
 * val ref = ::parFilterNot
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parFilterNot(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    @BuilderInference crossinline predicate: (T) -> Boolean,
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
 * val ref = ::parCount
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parCount(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    @BuilderInference crossinline predicate: (T) -> Boolean,
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
 * val ref = ::parForEach
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parForEach(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    @BuilderInference crossinline action: (T) -> Unit,
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
 * val ref = ::parForEachWithIndex
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parForEachWithIndex(
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    @BuilderInference crossinline action: (index: Int, element: T) -> Unit,
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
 * val ref = ::parForEachWithIndex
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Iterable<T>.parForEachWithIndex(
    minForSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    taskCount: Int = AVAILABLE_PROCESSORS,
    @BuilderInference crossinline action: (index: Int, element: T) -> Unit,
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
 * val ref = ::parMap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, R> Iterable<T>.parMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    @BuilderInference crossinline mapper: (T) -> R,
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
 * val ref = ::parFlatMap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, R> Iterable<T>.parFlatMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    @BuilderInference crossinline mapper: (element: T) -> Collection<R>,
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
 * val ref = ::parFilterMap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, R> Iterable<T>.parFilterMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = DEFAULT_REORDER,
    crossinline predicate: (T) -> Boolean,
    @BuilderInference crossinline mapper: (T) -> R,
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
 * val ref = ::parGroupBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K, V> Iterable<V>.parGroupBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    @BuilderInference crossinline keyFunction: (V) -> K,
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
 * val ref = ::parAggregateBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, K, V> Iterable<T>.parAggregateBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    @BuilderInference crossinline groupBy: (T) -> K,
    @BuilderInference crossinline zeroValueFactory: () -> V,
    @BuilderInference crossinline nonMutatingAggregator: (V, T) -> V,
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
 * val ref = ::parAggregateInPlaceBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, K, V> Iterable<T>.parAggregateInPlaceBy(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    concurrentMultimap: MutableMultimap<K, V> = SynchronizedPutFastListMultimap.newMultimap<K, V>(),
    @BuilderInference crossinline groupBy: (T) -> K,
    @BuilderInference crossinline zeroValueFactory: () -> V,
    @BuilderInference crossinline mutatingAggregator: (V, T) -> Unit,
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
 * val ref = ::parSumBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    @BuilderInference crossinline groupBy: (T) -> V,
    @BuilderInference crossinline func: (T) -> Double,
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
 * val ref = ::parSumBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    @BuilderInference crossinline groupBy: (T) -> V,
    @BuilderInference crossinline func: (T) -> Float,
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
 * val ref = ::parSumBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    @BuilderInference crossinline groupBy: (T) -> V,
    @BuilderInference crossinline func: (T) -> Long,
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
 * val ref = ::parSumBy
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T, V> Iterable<T>.parSumBy(
    @BuilderInference crossinline groupBy: (T) -> V,
    @BuilderInference crossinline func: (T) -> Int,
): ObjectLongMap<V> =
    ParallelIterate.sumByInt(this, { groupBy(it) }, { func(it) })
