package io.bluetape4k.collections.eclipse.parallel

import org.eclipse.collections.impl.parallel.ParallelMapIterate
import java.util.concurrent.ExecutorService


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
inline fun <K, V> Map<K, V>.parForEach(
    @BuilderInference crossinline prcedure: (K, V) -> Unit,
) {
    ParallelMapIterate.forEachKeyValue(this) { k: K, v: V -> prcedure(k, v) }
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
inline fun <K, V, R> Map<K, V>.parMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    @BuilderInference crossinline mapper: (K, V) -> R,
): Collection<R> =
    asIterable().parMap(batchSize, executor, reorder) { (k: K, v: V) -> mapper(k, v) }

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
inline fun <K, V, R> Map<K, V>.parFlatMap(
    batchSize: Int = DEFAULT_PARALLEL_BATCH_SIZE,
    executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE,
    reorder: Boolean = false,
    @BuilderInference crossinline flatMapper: (K, V) -> Collection<R>,
): Collection<R> =
    asIterable().parFlatMap(batchSize, executor, reorder) { (k: K, v: V) -> flatMapper(k, v) }
