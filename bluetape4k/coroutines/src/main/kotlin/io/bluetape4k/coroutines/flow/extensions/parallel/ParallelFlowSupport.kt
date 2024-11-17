package io.bluetape4k.coroutines.flow.extensions.parallel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll


// -----------------------------------------------------------------------------------------
// Parallel Extensions
// -----------------------------------------------------------------------------------------

/**
 * upstream의 각 아이템을 병렬 flow의 병렬 rail로 소비합니다.
 *
 * ```
 * @Test
 * fun basic() = runTest {
 *     withParallels(1) { execs ->
 *         execs shouldHaveSize 1
 *         flowRangeOf(1, 5).log("source")
 *             .parallel(execs.size) { execs[it] }
 *             .reduce({ 0 }, { a, b -> a + b })
 *             .sequential().log("sequential")
 *             .assertResult(15)
 *     }
 * }
 * ```
 */
fun <T> Flow<T>.parallel(parallelism: Int, runOn: (Int) -> CoroutineDispatcher): ParallelFlow<T> =
    FlowParallel(this, parallelism, runOn)

/**
 * 병렬 upstream을 소비하고 다시 순차적인 flow로 변환합니다.
 */
fun <T> ParallelFlow<T>.sequential(): Flow<T> =
    FlowSequential(this)

/**
 *  [ParallelFlow] 의 요소들을 변환합니다.
 *
 * ```
 * @Test
 * fun `map - parallisim is 2`() = runSuspendTest {
 *     withParallels(2) { execs ->
 *         flowRangeOf(1, 5).log("source")
 *             .parallel(execs.size) { execs[it] }
 *             .map { it + 1 }
 *             .sequential().log("sequential")
 *             .assertResultSet(2, 3, 4, 5, 6)
 *     }
 * }
 *  ```
 */
fun <T, R> ParallelFlow<T>.map(mapper: suspend (T) -> R): ParallelFlow<R> =
    FlowParallelMap(this, mapper)

/**
 *  [ParallelFlow] 의 요소들을 필터링 합니다.
 */
fun <T> ParallelFlow<T>.filter(predicate: suspend (T) -> Boolean): ParallelFlow<T> =
    FlowParallelFilter(this, predicate)

/**
 * 각각의 upstream 아이템을 다수의 downstream에 대해 병렬로 emit으로 변환합니다.
 */
fun <T, R> ParallelFlow<T>.transform(callback: suspend FlowCollector<R>.(T) -> Unit): ParallelFlow<R> =
    FlowParallelTransform(this, callback)


/**
 * upstream 아이템을 각각 매핑하고, 그 값을 순서대로 emit 합니다.
 *
 * ```
 * withParallels(1) { execs ->
 *     execs shouldHaveSize 1
 *     flowRangeOf(1, 5).log("source")
 *         .parallel(execs.size) { execs[it] }
 *         .concatMap {
 *             log.trace { "item=$it" }
 *             flowOf(it + 1)
 *         }
 *         .sequential().log("sequential")
 *         .assertResult(2, 3, 4, 5, 6)
 * }
 * ```
 */
@ExperimentalCoroutinesApi
fun <T, R> ParallelFlow<T>.concatMap(mapper: suspend (T) -> Flow<R>): ParallelFlow<R> =
    FlowParallelTransform(this) {
        emitAll(mapper(it))
    }

/**
 * Source의 요소들을 하나의 값으로 [combine]하여 emit 합니다.
 *
 * ```
 * withParallels(1) { execs ->
 *     emptyFlow<Int>()
 *         .parallel(execs.size) { execs[it] }
 *         .reduce({ 0 }) { a, b ->
 *             log.trace { "a=$a, b=$b" }
 *             a + b
 *         }
 *         .sequential()
 *         .assertResult(0)
 * }
 * ```
 *
 * @see [FlowParallelReduce]
 */
fun <T, R> ParallelFlow<T>.reduce(seed: suspend () -> R, combine: suspend (R, T) -> R): ParallelFlow<R> =
    FlowParallelReduce(this, seed, combine)

/**
 * 병렬 [Flow]의 요소들을 하나의 값으로 [combine]하여 emit 하는 [Flow]를 반환합니다.
 *
 * ```
 * withParallels(2) { execs ->
 *     execs shouldHaveSize 2
 *     flowRangeOf(1, 5).log("source")
 *         .parallel(execs.size) { execs[it] }
 *         .reduce { a, b ->
 *             log.trace { "a=$a, b=$b" }
 *             a + b
 *         }
 *         .log("reduce")
 *         .assertResult(15)
 * }
 * ```
 *
 * @see [FlowParallelReduceSequential]
 */
fun <T> ParallelFlow<T>.reduce(combine: suspend (T, T) -> T): Flow<T> =
    FlowParallelReduceSequential(this, combine)
