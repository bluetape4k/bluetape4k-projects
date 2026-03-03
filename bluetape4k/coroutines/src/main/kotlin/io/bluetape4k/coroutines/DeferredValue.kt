package io.bluetape4k.coroutines

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.ValueObject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * 비동기 계산 결과를 값 객체처럼 다루기 위한 래퍼입니다.
 *
 * ## 동작/계약
 * - 생성 시 [valueSupplier]를 `async`로 즉시 시작합니다.
 * - `value`는 필요 시 `runBlocking`으로 완료를 기다리므로, 호출 스레드를 블로킹할 수 있습니다.
 * - `await()`는 suspend 방식으로 결과를 기다리며 예외/취소를 그대로 전파합니다.
 * - 완료된 값이 있으면 equals/hashCode는 값 기준, 미완료면 Deferred 참조 기준으로 동작합니다.
 *
 * ```kotlin
 * val deferred = deferredValueOf { 21 * 2 }
 * val value = deferred.value
 * // value == 42
 * ```
 * @param valueSupplier 결과 값을 계산하는 suspend 함수입니다.
 */
class DeferredValue<T: Any>(
    internal val valueSupplier: suspend () -> T,
): DefaultCoroutineScope(), ValueObject {

    private val _value: Deferred<T> = async { valueSupplier() }

    /**
     * 계산 결과 값을 동기 방식으로 가져옵니다.
     *
     * ## 동작/계약
     * - 완료된 Deferred면 즉시 값을 반환합니다.
     * - 미완료 상태면 `runBlocking`으로 완료까지 대기합니다.
     * - 취소되었거나 실패한 Deferred는 해당 예외를 전파합니다.
     *
     * > **경고**: 코루틴 내부에서 이 프로퍼티를 호출하면 `Dispatchers.Default` 스레드 풀이
     * > 고갈될 경우 데드락이 발생할 수 있습니다. **코루틴 환경에서는 반드시 `await()`를 사용하세요.**
     *
     * ```kotlin
     * val deferred = deferredValueOf { 42 }
     * val value = deferred.value  // 비코루틴 컨텍스트에서만 사용
     * val valueSafe = deferred.await()  // 코루틴 내부에서 사용
     * // value == 42
     * ```
     */
    val value: T by lazy {
        if (_value.isCompleted && !_value.isCancelled) {
            _value.getCompleted()
        } else {
            runBlocking { _value.await() }
        }
    }

    /**
     * 계산 결과를 suspend 방식으로 기다립니다.
     *
     * ## 동작/계약
     * - 호출 코루틴을 블로킹하지 않고 Deferred 완료를 기다립니다.
     * - 계산 예외와 취소는 그대로 전파됩니다.
     * - 완료 후 재호출 시 동일한 결과를 반환합니다.
     *
     * ```kotlin
     * val deferred = deferredValueOf { 42 }
     * val value = deferred.await()
     * // value == 42
     * ```
     */
    suspend fun await(): T = _value.await()

    /**
     * 내부 Deferred가 완료되었는지 반환합니다.
     *
     * ## 동작/계약
     * - 완료 여부만 조회하며 상태를 변경하지 않습니다.
     * - 성공/실패/취소 완료를 모두 `true`로 반환합니다.
     *
     * ```kotlin
     * val deferred = deferredValueOf { 42 }
     * val done = deferred.isCompleted
     * // done == false || true
     * ```
     */
    val isCompleted: Boolean get() = _value.isCompleted

    /**
     * 내부 Deferred가 활성 상태인지 반환합니다.
     *
     * ## 동작/계약
     * - 계산이 아직 완료/취소되지 않았으면 `true`입니다.
     * - 조회 전용 프로퍼티이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val deferred = deferredValueOf { 42 }
     * val active = deferred.isActive
     * // active == true || false
     * ```
     */
    val isActive: Boolean get() = _value.isActive

    /**
     * 내부 Deferred가 취소되었는지 반환합니다.
     *
     * ## 동작/계약
     * - 취소 완료된 경우에만 `true`를 반환합니다.
 * - 조회 전용 프로퍼티이며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val deferred = deferredValueOf { 42 }
     * val cancelled = deferred.isCancelled
     * // cancelled == false || true
     * ```
     */
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
 * suspend 계산식으로 [DeferredValue]를 생성합니다.
 *
 * ## 동작/계약
 * - [DeferredValue] 생성과 동시에 계산이 시작됩니다.
 * - 별도 검증 없이 [valueSupplier]를 그대로 사용합니다.
 * - 계산 중 예외는 `value`/`await()` 호출 시점에 전파됩니다.
 *
 * ```kotlin
 * val deferred = deferredValueOf { 40 + 2 }
 * val value = deferred.await()
 * // value == 42
 * ```
 * @param valueSupplier 결과 값을 계산하는 suspend 함수입니다.
 */
fun <T: Any> deferredValueOf(valueSupplier: suspend () -> T): DeferredValue<T> = DeferredValue(valueSupplier)

/**
 * [DeferredValue] 결과에 변환 함수를 적용한 새 [DeferredValue]를 만듭니다.
 *
 * ## 동작/계약
 * - 원본 값을 `await()`로 얻은 뒤 [transform]을 적용합니다.
 * - 원본/변환 계산 중 예외는 결과 DeferredValue를 소비할 때 전파됩니다.
 * - 수신 객체를 변경하지 않고 새 DeferredValue를 반환합니다.
 *
 * ```kotlin
 * val mapped = deferredValueOf { 21 }.map { it * 2 }
 * val value = mapped.await()
 * // value == 42
 * ```
 * @param transform 원본 값을 다른 값으로 변환하는 suspend 함수입니다.
 */
inline fun <T: Any, S: Any> DeferredValue<T>.map(crossinline transform: suspend (T) -> S): DeferredValue<S> =
    DeferredValue { transform(await()) }

/**
 * [DeferredValue] 결과를 다른 [DeferredValue]로 평탄화합니다.
 *
 * ## 동작/계약
 * - 수신 값을 기다린 뒤 [flatter]가 반환한 DeferredValue를 다시 기다립니다.
 * - 두 단계 계산 중 발생한 예외/취소는 최종 결과에서 전파됩니다.
 * - 수신 객체를 변경하지 않고 새 DeferredValue를 반환합니다.
 *
 * ```kotlin
 * val flatMapped = deferredValueOf { 21 }.flatMap { deferredValueOf { it * 2 } }
 * val value = flatMapped.await()
 * // value == 42
 * ```
 * @param flatter 원본 값을 다음 DeferredValue로 연결하는 함수입니다.
 */
inline fun <T: Any, S: Any> DeferredValue<T>.flatMap(crossinline flatter: (T) -> DeferredValue<S>): DeferredValue<S> =
    DeferredValue { flatter(await()).await() }
