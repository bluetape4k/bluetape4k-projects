package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.FlowNoElementException

/**
 * Flow의 값/오류/완료를 값 객체로 표현한 이벤트 타입입니다.
 *
 * ## 동작/계약
 * - [Value], [Error], [Complete] 3가지 상태를 명시적으로 표현합니다.
 * - 이벤트 객체는 불변이며 상태를 변경하지 않습니다.
 * - 변환 함수(`map`, `flatMap` 등)는 Value일 때만 변환을 적용하고 Error/Complete는 그대로 전달합니다.
 *
 * ```kotlin
 * val event: FlowEvent<Int> = FlowEvent.Value(1)
 * // event == FlowEvent.Value(1)
 * ```
 */
sealed interface FlowEvent<out T> {

    /** 값을 가진 이벤트입니다. */
    data class Value<out T>(val value: T): FlowEvent<T> {
        override fun toString(): String = "FlowEvent.Value($value)"
    }

    /** 오류를 가진 이벤트입니다. */
    data class Error(val error: Throwable): FlowEvent<Nothing> {
        override fun toString(): String = "FlowEvent.Error($error)"
    }

    /** 정상 완료 이벤트입니다. */
    data object Complete: FlowEvent<Nothing> {
        override fun toString(): String = "FlowEvent.Complete"
    }
}

/**
 * Value 이벤트에만 변환 함수를 적용합니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `Value(transform(v))`를 반환합니다.
 * - `Error`, `Complete`는 변경 없이 그대로 반환합니다.
 * - [transform] 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = FlowEvent.Value(1).map { it + 1 }
 * // out == FlowEvent.Value(2)
 * ```
 * @param transform Value에 적용할 변환 함수입니다.
 */
inline fun <T, R> FlowEvent<T>.map(transform: (T) -> R): FlowEvent<R> = when (this) {
    is FlowEvent.Value -> FlowEvent.Value(transform(value))
    is FlowEvent.Error -> this
    FlowEvent.Complete -> FlowEvent.Complete
}

/**
 * Value 이벤트를 다른 FlowEvent로 평탄화합니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `transform(v)` 결과를 반환합니다.
 * - `Error`, `Complete`는 변경 없이 그대로 반환합니다.
 * - [transform] 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = FlowEvent.Value(1).flatMap { FlowEvent.Value(it + 1) }
 * // out == FlowEvent.Value(2)
 * ```
 * @param transform Value를 다른 FlowEvent로 변환하는 함수입니다.
 */
inline fun <T, R> FlowEvent<T>.flatMap(transform: (T) -> FlowEvent<R>): FlowEvent<R> = when (this) {
    is FlowEvent.Value -> transform(value)
    is FlowEvent.Error -> this
    FlowEvent.Complete -> FlowEvent.Complete
}

/**
 * Value가 아니면 기본값 생성 함수를 사용해 값을 반환합니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `v`를 반환합니다.
 * - `Error(e)`면 `defaultValue(e)`를 반환합니다.
 * - `Complete`면 `defaultValue(null)`을 반환합니다.
 *
 * ```kotlin
 * val out = FlowEvent.Error(RuntimeException("boom")).valueOrElse { 2 }
 * // out == 2
 * ```
 * @param defaultValue Value가 아닐 때 대체 값을 계산하는 함수입니다.
 */
inline fun <T> FlowEvent<T>.valueOrElse(defaultValue: (Throwable?) -> T): T = when (this) {
    is FlowEvent.Value -> value
    is FlowEvent.Error -> defaultValue(error)
    FlowEvent.Complete -> defaultValue(null)
}

/**
 * Value면 값을, 아니면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `v`, 그 외에는 `null`을 반환합니다.
 * - 상태를 변경하지 않는 조회 연산입니다.
 *
 * ```kotlin
 * val out = FlowEvent.Complete.valueOrNull<Int>()
 * // out == null
 * ```
 */
fun <T> FlowEvent<T>.valueOrNull(): T? = valueOrDefault(null)

/**
 * Value가 아니면 [defaultValue]를 반환합니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `v`를 반환합니다.
 * - `Error`, `Complete`면 [defaultValue]를 반환합니다.
 * - 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val out = FlowEvent.Complete.valueOrDefault(2)
 * // out == 2
 * ```
 * @param defaultValue 대체 기본값입니다.
 */
fun <T> FlowEvent<T>.valueOrDefault(defaultValue: T): T = valueOrElse { defaultValue }

/**
 * Value를 반환하고, Value가 아니면 예외를 던집니다.
 *
 * ## 동작/계약
 * - `Value(v)`면 `v`를 반환합니다.
 * - `Error(e)`면 `e`를 던집니다.
 * - `Complete`면 [FlowNoElementException]을 던집니다.
 *
 * ```kotlin
 * val out = FlowEvent.Value(1).valueOrThrow()
 * // out == 1
 * ```
 */
fun <T> FlowEvent<T>.valueOrThrow(): T =
    valueOrElse { throw (it ?: FlowNoElementException("$this has no value!")) }


/**
 * Error 이벤트의 예외를 반환하고, 아니면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `Error(e)`면 `e`를 반환합니다.
 * - `Value`, `Complete`면 `null`을 반환합니다.
 * - 상태를 변경하지 않는 조회 연산입니다.
 *
 * ```kotlin
 * val ex = FlowEvent.Error(RuntimeException("boom")).errorOrNull()
 * // ex?.message == "boom"
 * ```
 */
fun <T> FlowEvent<T>.errorOrNull(): Throwable? = when (this) {
    is FlowEvent.Value -> null
    is FlowEvent.Error -> error
    FlowEvent.Complete -> null
}

/**
 * Error 이벤트의 예외를 반환하고, Error가 아니면 예외를 던집니다.
 *
 * ## 동작/계약
 * - `Error(e)`면 `e`를 반환합니다.
 * - `Value`, `Complete`면 [FlowNoElementException]을 던집니다.
 * - 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val ex = FlowEvent.Error(RuntimeException("boom")).errorOrThrow()
 * // ex.message == "boom"
 * ```
 */
fun <T> FlowEvent<T>.errorOrThrow(): Throwable =
    errorOrNull() ?: throw FlowNoElementException("$this has no error!")
