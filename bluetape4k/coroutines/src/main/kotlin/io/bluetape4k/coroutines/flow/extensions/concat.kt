package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [f1], [f2], [fs] 순서대로 collect 합니다.
 *
 * ```
 * val flow1 = flowOf(1, 2, 3)
 * val flow2 = flowOf(4, 5, 6)
 * val flow3 = flowOf(7, 8, 9)
 * val flow4 = flowOf(10, 11, 12)
 * concat(flow1, flow2, flow3, flow4)  // 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
 * ```
 */
fun <T> concat(f1: Flow<T>, f2: Flow<T>, vararg fs: Flow<T>): Flow<T> = flow {
    emitAll(f1)
    emitAll(f2)
    fs.forEach { f ->
        emitAll(f)
    }
}

/**
 * source flow 를 모두 collect 하고난 후, [f1], [fs] 를 collect 합니다.
 *
 * ```
 * val flow1 = flowOf(1, 2, 3)
 * flow1.concatWith(flowOf(4, 5, 6), flowOf(7, 8, 9))  // 1, 2, 3, 4, 5, 6, 7, 8, 9
 * ```
 */
fun <T> Flow<T>.concatWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> =
    concat(this, f1, *fs)

/**
 * [Flow]들을 순차적으로 collect 합니다.
 *
 * ```
 * val flow1 = flowOf(1, 2, 3)
 * val flow2 = flowOf(4, 5, 6)
 * val flow3 = flowOf(7, 8, 9)
 * listOf(flow1, flow2, flow3).concat()  // 1, 2, 3, 4, 5, 6, 7, 8, 9
 * ```
 */
fun <T> Iterable<Flow<T>>.concat(): Flow<T> = flow {
    forEach { f -> emitAll(f) }
}

/**
 * [Flow] 의 첫번째 emit 요소로 [item], [items]들을 추가합니다. (prepend 와 유사)
 *
 * ```
 * flowOf(2, 3, 4).startWith(1)  // 1, 2, 3, 4
 * flowOf(2, 3, 4).startWith(1, 0)  // 1, 0, 2, 3, 4
 * ```
 *
 * @param item 선두에 추가할 요소
 * @param items 선두에 추가할 요소들
 */
fun <T> Flow<T>.startWith(item: T, vararg items: T): Flow<T> =
    concat(
        flow {
            emit(item)
            emitAll(items.asFlow())
        },
        this
    )

/**
 * [valueSupplier] 를 통해 값을 생성하여, [Flow]의 첫 요소로 추가합니다.
 *
 * ```
 * flowOf(2, 3, 4).startWith { 1 }  // 1, 2, 3, 4
 * flowOf(2, 3, 4).startWith { 1 + 1 }  // 2, 2, 3, 4
 * ```
 *
 * @param valueSupplier 선두에 추가할 요소를 생성하는 suspend 함수
 */
fun <T> Flow<T>.startWith(valueSupplier: suspend () -> T): Flow<T> =
    concat(
        flow { emit(valueSupplier()) },
        this
    )

/**
 * [f1], [fs] 를 순차적으로 collect 한 후, source [Flow]를 collect 합니다.
 *
 * ```
 * val flow1 = flowOf(1, 2, 3)
 * val flow2 = flowOf(4, 5, 6)
 * val flow3 = flowOf(7, 8, 9)
 * flow1.startWith(flow2, flow3)  // 4, 5, 6, 7, 8, 9, 1, 2, 3
 * ```
 *
 * @param f1 선두에 추가할 [Flow]
 * @param fs 선두에 추가할 [Flow]들
 */
fun <T> Flow<T>.startWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> = flow {
    emitAll(f1)
    fs.forEach { emitAll(it) }
    emitAll(this@startWith)
}

/**
 * source [Flow]가 emit 하고, 순차적으로 [item], [items]들을 emit 한다 (append 와 유사)
 *
 * ```
 * flowOf(1, 2, 3).endWith(4)  // 1, 2, 3, 4
 * flowOf(1, 2, 3).endWith(4, 5)  // 1, 2, 3, 4, 5
 * ```
 *
 * @param item 후미에 추가할 요소
 * @param items 후미에 추가할 요소들
 *
 * @see [concat]
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.endWith(item: T, vararg items: T): Flow<T> =
    concat(
        this,
        flow {
            emit(item)
            emitAll(items.asFlow())
        })

/**
 * source [Flow]가 emit 하고, 순차적으로 [f1], [fs]들을 emit 한다. [concatWith] 와 같다
 *
 * ```
 * val flow1 = flowOf(1, 2, 3)
 * val flow2 = flowOf(4, 5, 6)
 * val flow3 = flowOf(7, 8, 9)
 * flow1.endWith(flow2, flow3)  // 1, 2, 3, 4, 5, 6, 7, 8, 9
 * ```
 *
 * @param f1 후미에 추가할 [Flow]
 * @param fs 후미에 추가할 [Flow]들
 * @see [concatWith]
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.endWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> = concatWith(f1, *fs)
