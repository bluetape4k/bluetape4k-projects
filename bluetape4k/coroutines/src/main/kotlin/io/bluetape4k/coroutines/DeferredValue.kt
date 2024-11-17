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
class DeferredValue<T>(
    internal val valueSupplier: suspend () -> T,
): DefaultCoroutineScope(), ValueObject {

    private val _value: Deferred<T> = async { valueSupplier() }

    val value: T by lazy {
        runBlocking { _value.await() }
    }

    suspend fun await(): T = _value.await()

    val isCompleted: Boolean get() = _value.isCompleted
    val isActive: Boolean get() = _value.isActive
    val isCancelled: Boolean get() = _value.isCancelled

    override fun equals(other: Any?): Boolean {
        return other is DeferredValue<*> && value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String {
        return ToStringBuilder(this)
            .add("value", value)
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
fun <T> deferredValueOf(valueSupplier: suspend () -> T): DeferredValue<T> = DeferredValue(valueSupplier)

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
inline fun <T, S> DeferredValue<T>.map(crossinline transform: suspend (T) -> S): DeferredValue<S> =
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
inline fun <T, S> DeferredValue<T>.flatMap(crossinline flatter: (T) -> DeferredValue<S>): DeferredValue<S> =
    DeferredValue { flatter(await()).await() }
