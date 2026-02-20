package io.bluetape4k.coroutines

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.ValueObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * 값 계산을 지연해서 수행하는 클래스입니다.
 *
 * ```
 * val deferredValue = DeferredValue { delay(1000); 1 }
 * val value = deferredValue.value
 * ```
 *
 * @property valueSupplier 값을 생성하는 suspend 함수
 */
class DeferredValue<T: Any>(
    internal val valueSupplier: suspend () -> T,
): DefaultCoroutineScope(), ValueObject {

    private val _value: Deferred<T> = async { valueSupplier() }

    val value: T by lazy {
        if (_value.isCompleted && !_value.isCancelled) {
            _value.getCompleted()
        } else {
            runBlocking { _value.await() }
        }
    }

    suspend fun await(): T = _value.await()

    val isCompleted: Boolean get() = _value.isCompleted
    val isActive: Boolean get() = _value.isActive
    val isCancelled: Boolean get() = _value.isCancelled

    private fun completedValueOrNull(): T? =
        if (isCompleted && !isCancelled) {
            runCatching { _value.getCompleted() }.getOrNull()
        } else {
            null
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeferredValue<*>) return false

        val thisValue = completedValueOrNull()
        val otherValue = other.completedValueOrNull()
        if (thisValue != null || otherValue != null) {
            return thisValue == otherValue
        }
        return _value === other._value
    }

    override fun hashCode(): Int =
        completedValueOrNull()?.hashCode() ?: System.identityHashCode(_value)

    override fun toString(): String {
        val completedValue = completedValueOrNull()
        return ToStringBuilder(this)
            .add("isCompleted", isCompleted)
            .add("isActive", isActive)
            .add("isCancelled", isCancelled)
            .add("value", completedValue)
            .toString()
    }
}

/**
 * 값을 지연해서 생성하는 DeferredValue를 생성합니다.
 *
 * ```
 * val deferredValue = deferredValueOf { delay(1000); 1 }
 * val value = deferredValue.value
 * ```
 *
 * @param valueSupplier 값을 생성하는 suspend 함수
 */
fun <T: Any> deferredValueOf(valueSupplier: suspend () -> T): DeferredValue<T> = DeferredValue(valueSupplier)

/**
 * [DeferredValue] 의 지연된 계산 값이 완료되면 [transform]으로 변환한 값을 반환합니다.
 *
 * ```
 * val deferredValue = deferredValueOf { delay(1000); 1 }
 * val transformed = deferredValue.map { it * 2 }           // 2
 * ```
 *
 * @param transform 변환 함수
 * @return 변환된 DeferredValue
 */
inline fun <T: Any, S: Any> DeferredValue<T>.map(crossinline transform: suspend (T) -> S): DeferredValue<S> =
    DeferredValue { transform(await()) }

/**
 * [DeferredValue] 의 지연된 계산 값이 완료되면 [flatter]로 변환한 [DeferredValue]의 값을 반환합니다.
 *
 * ```
 * val deferredValue = deferredValueOf { delay(1000); 1 }
 * val transformed = deferredValue.flatMap { deferredValueOf { it * 2 } }           // 2
 * ```
 *
 * @param flatter 변환 함수
 * @return 변환된 DeferredValue
 */
inline fun <T: Any, S: Any> DeferredValue<T>.flatMap(crossinline flatter: (T) -> DeferredValue<S>): DeferredValue<S> =
    DeferredValue { flatter(await()).await() }
