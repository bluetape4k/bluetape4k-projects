package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.StopException
import io.bluetape4k.coroutines.flow.exceptions.checkOwnership
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/**
 * Flow 시그널을 값 이벤트([FlowEvent])로 물질화(materialize)합니다.
 *
 * ## 동작/계약
 * - 일반 값은 `FlowEvent.Value`, 정상 완료는 `FlowEvent.Complete`, 예외 종료는 `FlowEvent.Error`로 변환합니다.
 * - 결과 Flow는 예외를 던지지 않고 이벤트 값으로 상태를 전달합니다.
 * - 요소당 이벤트 객체가 생성됩니다.
 *
 * ```kotlin
 * val result = flowOf(1, 2).materialize().toList()
 * // result == [Value(1), Value(2), Complete]
 * ```
 */
fun <T> Flow<T>.materialize(): Flow<FlowEvent<T>> =
    map<T, FlowEvent<T>> { FlowEvent.Value(it) }
        .onCompletion { if (it == null) emit(FlowEvent.Complete) }
        .catch { ex -> emit(FlowEvent.Error(ex)) }

/**
 * [FlowEvent] 스트림을 실제 값 스트림으로 역변환(dematerialize)합니다.
 *
 * ## 동작/계약
 * - `Value`는 원래 값으로 방출합니다.
 * - `Error`는 담긴 예외를 즉시 던집니다.
 * - `Complete`를 만나면 내부 `StopException`으로 현재 dematerialize 루프만 중단합니다.
 *
 * ```kotlin
 * val events = flowOf(FlowEvent.Value(1), FlowEvent.Complete)
 * val result = events.dematerialize().toList()
 * // result == [1]
 * ```
 */
fun <T> Flow<FlowEvent<T>>.dematerialize(): Flow<T> = flow {
    try {
        collect {
            when (it) {
                is FlowEvent.Value -> emit(it.value)
                is FlowEvent.Error -> throw it.error
                FlowEvent.Complete -> throw StopException(this)
            }
        }
    } catch (e: StopException) {
        e.checkOwnership(this@flow)
    }
}
