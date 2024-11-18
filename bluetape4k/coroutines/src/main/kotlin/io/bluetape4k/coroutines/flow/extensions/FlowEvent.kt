package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.flow.exceptions.FlowNoElementException

/**
 * Reactive signal types: value, error and complete 을 가진 signal 정보를 제공합니다.
 *
 * Flow를 ReactiveX의 Observable과 비슷한 방식으로 사용할 수 있도록 합니다.
 *
 * @see [io.bluetape4k.coroutines.flow.extensions.materialize]
 * @see [io.bluetape4k.coroutines.flow.extensions.dematerialize]
 */
sealed interface FlowEvent<out T> {

    data class Value<out T>(val value: T): FlowEvent<T> {
        override fun toString(): String = "FlowEvent.Value($value)"
    }

    data class Error(val error: Throwable): FlowEvent<Nothing> {
        override fun toString(): String = "FlowEvent.Error($error)"
    }

    data object Complete: FlowEvent<Nothing> {
        override fun toString(): String = "FlowEvent.Complete"
    }
}

inline fun <T, R> FlowEvent<T>.map(transform: (T) -> R): FlowEvent<R> = when (this) {
    is FlowEvent.Value -> FlowEvent.Value(transform(value))
    is FlowEvent.Error -> this
    FlowEvent.Complete -> FlowEvent.Complete
}

inline fun <T, R> FlowEvent<T>.flatMap(transform: (T) -> FlowEvent<R>): FlowEvent<R> = when (this) {
    is FlowEvent.Value -> transform(value)
    is FlowEvent.Error -> this
    FlowEvent.Complete -> FlowEvent.Complete
}

inline fun <T> FlowEvent<T>.valueOrElse(defaultValue: (Throwable?) -> T): T = when (this) {
    is FlowEvent.Value -> value
    is FlowEvent.Error -> defaultValue(error)
    FlowEvent.Complete -> defaultValue(null)
}

fun <T> FlowEvent<T>.valueOrNull(): T? = valueOrDefault(null)

fun <T> FlowEvent<T>.valueOrDefault(defaultValue: T): T = valueOrElse { defaultValue }

fun <T> FlowEvent<T>.valueOrThrow(): T =
    valueOrElse { throw (it ?: FlowNoElementException("$this has no value!")) }


fun <T> FlowEvent<T>.errorOrNull(): Throwable? = when (this) {
    is FlowEvent.Value -> null
    is FlowEvent.Error -> error
    FlowEvent.Complete -> null
}

fun <T> FlowEvent<T>.errorOrThrow(): Throwable =
    errorOrNull() ?: throw FlowNoElementException("$this has no error!")
