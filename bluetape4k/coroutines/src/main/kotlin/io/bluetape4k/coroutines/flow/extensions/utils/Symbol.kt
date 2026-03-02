package io.bluetape4k.coroutines.flow.extensions.utils

/**
 * sentinel 용도로 사용하는 문자열 래퍼 값 객체입니다.
 *
 * ## 동작/계약
 * - 일반 데이터와 구분되는 식별 토큰을 표현하기 위해 사용합니다.
 * - `toString()`은 `<symbol>` 형태로 렌더링합니다.
 * - 내부 상태는 불변이며 생성 후 변경되지 않습니다.
 *
 * ```kotlin
 * val done = Symbol("DONE")
 * // done.toString() == "<DONE>"
 * ```
 *
 * @property symbol sentinel 식별 문자열입니다.
 */
data class Symbol(@JvmField val symbol: String) {
    override fun toString(): String = "<$symbol>"

    /**
     * 전달값이 현재 Symbol sentinel이면 `null`로, 아니면 지정 타입으로 언박싱합니다.
     *
     * ## 동작/계약
     * - `value === this`이면 `null as T`를 반환합니다.
     * - 그 외에는 `value as T` 캐스팅 결과를 반환합니다.
     * - 타입이 맞지 않으면 `ClassCastException`이 발생할 수 있습니다.
     *
     * ```kotlin
     * val done = Symbol("DONE")
     * val a: Int? = done.unbox(done)
     * val b: Int = done.unbox(10)
     * // a == null, b == 10
     * ```
     *
     * @param value 언박싱 대상 값입니다.
     */
    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    inline fun <T> unbox(value: Any?): T = if (value === this) null as T else value as T
}
