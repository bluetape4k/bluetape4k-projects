package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll


// -----------------------------------------------------------------------------------------
// Parallel Extensions
// -----------------------------------------------------------------------------------------

/**
 * Flow를 병렬 rail로 분할 처리하는 [ParallelFlow]를 생성합니다.
 *
 * ## 동작/계약
 * - [parallelism]은 1 이상이어야 하며, 아니면 `IllegalArgumentException`이 발생합니다.
 * - 각 rail은 `runOn(index)`가 반환한 dispatcher에서 실행됩니다.
 * - 실제 수집 시 collector 수가 parallelism과 다르면 예외가 발생합니다(구현 검증).
 * - 수신 Flow를 변경하지 않고 병렬 처리 래퍼를 반환합니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2, 3).parallel(1) { Dispatchers.Default }
 * // pf.parallelism == 1
 * ```
 * @param parallelism 병렬 rail 개수입니다. 1 이상이어야 합니다.
 * @param runOn rail index별 실행 dispatcher를 반환하는 함수입니다.
 */
fun <T> Flow<T>.parallel(parallelism: Int, runOn: (Int) -> CoroutineDispatcher): ParallelFlow<T> =
    FlowParallel(this, parallelism.requirePositiveNumber("parallelism"), runOn)

/**
 * 병렬 rail 결과를 단일 순차 Flow로 합칩니다.
 *
 * ## 동작/계약
 * - 각 rail collector가 준비되는 순서에 따라 값을 순차로 재방출합니다.
 * - rail 중 하나에서 오류가 발생하면 수집 중 예외가 전파됩니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .parallel(1) { Dispatchers.Default }
 *     .sequential()
 *     .toList()
 * // out == [1, 2, 3]
 * ```
 */
fun <T> ParallelFlow<T>.sequential(): Flow<T> =
    FlowSequential(this)

/**
 * 병렬 rail마다 값을 변환합니다.
 *
 * ## 동작/계약
 * - 각 rail에서 [mapper]를 적용해 값을 변환합니다.
 * - mapper 예외는 수집 시점에 전파됩니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 ParallelFlow를 반환합니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2, 3)
 *     .parallel(1) { Dispatchers.Default }
 *     .map { it + 1 }
 * // pf.parallelism == 1
 * ```
 * @param mapper rail별 요소 변환 함수입니다.
 */
fun <T, R> ParallelFlow<T>.map(mapper: suspend (T) -> R): ParallelFlow<R> =
    FlowParallelMap(this, mapper)

/**
 * 병렬 rail마다 predicate를 적용해 값을 필터링합니다.
 *
 * ## 동작/계약
 * - 각 rail에서 [predicate]가 `true`인 값만 통과시킵니다.
 * - predicate 예외는 수집 시점에 전파됩니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 ParallelFlow를 반환합니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2, 3)
 *     .parallel(1) { Dispatchers.Default }
 *     .filter { it % 2 == 1 }
 * // pf.parallelism == 1
 * ```
 * @param predicate 필터 조건 함수입니다.
 */
fun <T> ParallelFlow<T>.filter(predicate: suspend (T) -> Boolean): ParallelFlow<T> =
    FlowParallelFilter(this, predicate)

/**
 * 병렬 rail마다 커스텀 transform 블록을 적용합니다.
 *
 * ## 동작/계약
 * - 각 rail에서 [callback]으로 0개 이상의 값을 방출할 수 있습니다.
 * - callback 예외는 수집 시점에 전파됩니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 ParallelFlow를 반환합니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2)
 *     .parallel(1) { Dispatchers.Default }
 *     .transform { emit(it); emit(it * 10) }
 * // pf.parallelism == 1
 * ```
 * @param callback rail별 transform 블록입니다.
 */
fun <T, R> ParallelFlow<T>.transform(callback: suspend FlowCollector<R>.(T) -> Unit): ParallelFlow<R> =
    FlowParallelTransform(this, callback)


/**
 * 병렬 rail마다 Flow를 매핑하고 각 rail 내부에서 순차 concat합니다.
 *
 * ## 동작/계약
 * - mapper가 반환한 Flow를 `emitAll`로 이어 붙입니다.
 * - rail 간 순서는 비결정적일 수 있지만, rail 내부는 mapper 결과 순서를 유지합니다.
 * - mapper/내부 Flow 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2)
 *     .parallel(1) { Dispatchers.Default }
 *     .concatMap { flowOf(it, it + 10) }
 * // pf.parallelism == 1
 * ```
 * @param mapper 각 요소를 Flow로 변환하는 함수입니다.
 */
@ExperimentalCoroutinesApi
fun <T, R> ParallelFlow<T>.concatMap(mapper: suspend (T) -> Flow<R>): ParallelFlow<R> =
    FlowParallelTransform(this) {
        emitAll(mapper(it))
    }

/**
 * rail별로 누적(reduce)한 결과를 다시 병렬 Flow로 반환합니다.
 *
 * ## 동작/계약
 * - 각 rail은 [seed]에서 초기 누적값을 만들고 [combine]으로 값을 누적합니다.
 * - rail별 reduce 결과는 parallelism 개수만큼(비어있는 rail 제외) 방출될 수 있습니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 ParallelFlow를 반환합니다.
 *
 * ```kotlin
 * val pf = flowOf(1, 2, 3)
 *     .parallel(1) { Dispatchers.Default }
 *     .reduce(seed = { 0 }) { acc, v -> acc + v }
 * // pf.parallelism == 1
 * ```
 * @param seed rail별 초기 누적값 생성 함수입니다.
 * @param combine 누적 함수입니다.
 */
fun <T, R> ParallelFlow<T>.reduce(seed: suspend () -> R, combine: suspend (R, T) -> R): ParallelFlow<R> =
    FlowParallelReduce(this, seed, combine)

/**
 * rail별 reduce 결과를 다시 하나로 합쳐 단일 Flow로 반환합니다.
 *
 * ## 동작/계약
 * - 각 rail 내부를 먼저 reduce한 뒤, rail 결과들을 다시 [combine]으로 순차 결합합니다.
 * - 모든 rail이 비어 있으면 아무 값도 방출하지 않고 완료합니다.
 * - 수신 ParallelFlow를 변경하지 않고 새 Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2, 3)
 *     .parallel(1) { Dispatchers.Default }
 *     .reduce { a, b -> a + b }
 *     .toList()
 * // out == [6]
 * ```
 * @param combine 누적 함수입니다.
 */
fun <T> ParallelFlow<T>.reduce(combine: suspend (T, T) -> T): Flow<T> =
    FlowParallelReduceSequential(this, combine)
