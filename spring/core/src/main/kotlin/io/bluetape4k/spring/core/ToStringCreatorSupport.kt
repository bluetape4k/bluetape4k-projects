package io.bluetape4k.spring.core

import org.springframework.core.style.DefaultToStringStyler
import org.springframework.core.style.DefaultValueStyler
import org.springframework.core.style.ToStringCreator
import org.springframework.core.style.ToStringStyler
import org.springframework.core.style.ValueStyler

/**
 * [ToStringCreator]에 빈 라인을 추가합니다.
 */
fun ToStringCreator.append(): ToStringCreatorAppendTokens = ToStringCreatorAppendTokens(this)

/**
 * [ToStringCreator]를 이용하여 겍체를 문자열로 표현할 수 있도록 합니다.
 *
 * ```kotlin
 * override fun toString(): String {
 *     return toStringCreatorOf(this) {
 *         append("name", name)
 *         append("age", age)
 *         append("birth", birth)
 *     }.toString()
 * }
 * ```
 *
 * @param obj 문자열로 표현할 객체
 * @param body 문자열로 표현할 코드
 * @return ToStringCreator
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    @BuilderInference body: ToStringCreator.() -> Unit,
): ToStringCreator {
    return ToStringCreator(obj).apply(body)
}

/**
 * [ToStringCreator]를 이용하여 겍체를 문자열로 표현할 수 있도록 합니다.
 *
 * ```kotlin
 * override fun toString(): String {
 *     return toStringCreatorOf(this) {
 *         append("name", name)
 *         append("age", age)
 *         append("birth", birth)
 *     }.toString()
 * }
 * ```
 *
 * @param obj 문자열로 표현할 객체
 * @param valueStyler 표현 방식
 * @param body 문자열로 표현할 코드
 * @return ToStringCreator
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    valueStyler: ValueStyler = DefaultValueStyler(),
    @BuilderInference body: ToStringCreator.() -> Unit,
): ToStringCreator {
    return ToStringCreator(obj, valueStyler).apply(body)
}

/**
 * [ToStringCreator]를 이용하여 겍체를 문자열로 표현할 수 있도록 합니다.
 *
 * ```
 * override fun toString(): String {
 *     return toStringCreatorOf(this, DefaultToStringStyler(DefaultValueStyler()) {
 *         append("name", name)
 *         append("age", age)
 *         append("birth", birth)
 *     }.toString()
 * }
 * ```
 *
 * @param obj 문자열로 표현할 객체
 * @param styler 표현 방식
 * @param body 문자열로 표현할 코드
 * @return ToStringCreator
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    styler: ToStringStyler = DefaultToStringStyler(DefaultValueStyler()),
    @BuilderInference body: ToStringCreator.() -> Unit,
): ToStringCreator =
    ToStringCreator(obj, styler).apply(body)

/**
 *
 *
 * @property creator
 */
class ToStringCreatorAppendTokens(private val creator: ToStringCreator) {

    operator fun set(fieldName: String, value: Boolean): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Byte): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Char): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Short): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Int): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Long): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Float): ToStringCreator = creator.append(fieldName, value)
    operator fun set(fieldName: String, value: Double): ToStringCreator = creator.append(fieldName, value)

    operator fun set(fieldName: String, value: Any?): ToStringCreator = creator.append(fieldName, value)
}
