package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * 여러 Flow를 순서대로 이어 붙인 Flow를 반환합니다.
 *
 * ## 동작/계약
 * - `f1 -> f2 -> fs...` 순서로 완전 수집한 뒤 다음 Flow를 수집합니다.
 * - 수신 객체를 변경하지 않고 새 cold Flow를 반환합니다.
 * - 앞선 Flow에서 예외가 발생하면 이후 Flow는 수집되지 않고 예외가 전파됩니다.
 *
 * ```kotlin
 * val out = concat(flowOf(1, 2), flowOf(3, 4)).toList()
 * // out == [1, 2, 3, 4]
 * ```
 * @param f1 첫 번째 Flow입니다.
 * @param f2 두 번째 Flow입니다.
 * @param fs 세 번째 이후 Flow들입니다.
 */
fun <T> concat(f1: Flow<T>, f2: Flow<T>, vararg fs: Flow<T>): Flow<T> = flow {
    emitAll(f1)
    emitAll(f2)
    fs.forEach { f ->
        emitAll(f)
    }
}

/**
 * 현재 Flow 뒤에 추가 Flow를 순차 연결합니다.
 *
 * ## 동작/계약
 * - 현재 Flow를 먼저 모두 방출한 뒤 `f1`, `fs`를 순차 방출합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 * - 연결 순서 중간에서 예외가 발생하면 이후 Flow는 수집되지 않습니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).concatWith(flowOf(3, 4)).toList()
 * // out == [1, 2, 3, 4]
 * ```
 * @param f1 뒤에 연결할 첫 Flow입니다.
 * @param fs 추가로 연결할 Flow들입니다.
 */
fun <T> Flow<T>.concatWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> =
    concat(this, f1, *fs)

/**
 * Flow 컬렉션을 순서대로 이어 붙입니다.
 *
 * ## 동작/계약
 * - Iterable 순회 순서대로 각 Flow를 완전 수집합니다.
 * - 빈 컬렉션이면 아무 값도 방출하지 않고 완료합니다.
 * - 수신 컬렉션/Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = listOf(flowOf(1, 2), flowOf(3, 4)).concat().toList()
 * // out == [1, 2, 3, 4]
 * ```
 */
fun <T> Iterable<Flow<T>>.concat(): Flow<T> = flow {
    forEach { f -> emitAll(f) }
}

/**
 * 현재 Flow 앞에 고정 값들을 먼저 방출합니다.
 *
 * ## 동작/계약
 * - `item`, `items`를 먼저 방출한 뒤 현재 Flow를 방출합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 * - vararg 값 개수만큼 추가 순회/방출이 발생합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).startWith(-1, 0).toList()
 * // out == [-1, 0, 1, 2]
 * ```
 * @param item 맨 앞에 붙일 첫 값입니다.
 * @param items 추가로 붙일 값들입니다.
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
 * 현재 Flow 앞에 동적 계산 값 1건을 붙입니다.
 *
 * ## 동작/계약
 * - 수집 시마다 [valueSupplier]를 호출해 값을 계산합니다.
 * - 계산된 값 1건 방출 후 현재 Flow를 이어서 방출합니다.
 * - [valueSupplier] 예외는 수집 시점에 전파됩니다.
 *
 * ```kotlin
 * var i = 0
 * val out = flowOf(2).startWith { ++i }.toList()
 * // out == [1, 2]
 * ```
 * @param valueSupplier 앞에 붙일 값을 생성하는 suspend 함수입니다.
 */
fun <T> Flow<T>.startWith(valueSupplier: suspend () -> T): Flow<T> =
    concat(
        flow { emit(valueSupplier()) },
        this
    )

/**
 * 현재 Flow 앞에 다른 Flow들을 먼저 방출합니다.
 *
 * ## 동작/계약
 * - `f1`, `fs`를 순서대로 완전 수집한 뒤 현재 Flow를 수집합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 * - 앞부분 Flow에서 예외가 발생하면 현재 Flow는 수집되지 않습니다.
 *
 * ```kotlin
 * val out = flowOf(5, 6).startWith(flowOf(1, 2), flowOf(3, 4)).toList()
 * // out == [1, 2, 3, 4, 5, 6]
 * ```
 * @param f1 앞에 붙일 첫 Flow입니다.
 * @param fs 추가로 앞에 붙일 Flow들입니다.
 */
fun <T> Flow<T>.startWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> = flow {
    emitAll(f1)
    fs.forEach { emitAll(it) }
    emitAll(this@startWith)
}

/**
 * 현재 Flow 뒤에 고정 값들을 이어 붙입니다.
 *
 * ## 동작/계약
 * - 현재 Flow를 모두 방출한 뒤 `item`, `items`를 순서대로 방출합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 * - vararg 값 개수만큼 추가 순회/방출이 발생합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).endWith(3, 4).toList()
 * // out == [1, 2, 3, 4]
 * ```
 * @param item 뒤에 붙일 첫 값입니다.
 * @param items 추가로 붙일 값들입니다.
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
 * 현재 Flow 뒤에 다른 Flow들을 순차 연결합니다.
 *
 * ## 동작/계약
 * - 동작은 [concatWith]와 동일합니다.
 * - 현재 Flow를 먼저 방출하고 `f1`, `fs`를 이어서 방출합니다.
 * - 수신 Flow를 변경하지 않고 새 cold Flow를 반환합니다.
 *
 * ```kotlin
 * val out = flowOf(1, 2).endWith(flowOf(3, 4)).toList()
 * // out == [1, 2, 3, 4]
 * ```
 * @param f1 뒤에 연결할 첫 Flow입니다.
 * @param fs 추가로 연결할 Flow들입니다.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.endWith(f1: Flow<T>, vararg fs: Flow<T>): Flow<T> = concatWith(f1, *fs)
