package io.bluetape4k.spring.core

import org.springframework.core.style.DefaultToStringStyler
import org.springframework.core.style.DefaultValueStyler
import org.springframework.core.style.ToStringCreator
import org.springframework.core.style.ToStringStyler
import org.springframework.core.style.ValueStyler

/**
 * 인덱스 연산자 기반 `append` 토큰 객체를 생성합니다.
 *
 * ## 동작/계약
 * - 수신 [ToStringCreator]를 감싼 [ToStringCreatorAppendTokens]를 반환합니다.
 * - 반환된 토큰의 `tokens["name"] = value` 문법은 내부적으로 `append("name", value)`를 호출합니다.
 *
 * ```kotlin
 * val creator = ToStringCreator(Any())
 * val tokens = creator.append()
 * tokens["value"] = 1
 * // creator.toString().contains("value") == true
 * ```
 */
fun ToStringCreator.append(): ToStringCreatorAppendTokens = ToStringCreatorAppendTokens(this)

/**
 * 기본 스타일로 [ToStringCreator]를 생성하고 본문을 적용합니다.
 *
 * ## 동작/계약
 * - [ToStringCreator] 인스턴스를 새로 만들고 [body]를 적용한 뒤 반환합니다.
 * - 테스트에서 생성된 문자열은 클래스명과 추가한 필드 문자열을 포함합니다.
 *
 * ```kotlin
 * val value = toStringCreatorOf(Any()) {
 *     append("age", 42)
 * }.toString()
 * // value.contains("age = 42") == true
 * ```
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    body: ToStringCreator.() -> Unit,
): ToStringCreator {
    return ToStringCreator(obj).apply(body)
}

/**
 * [ValueStyler]를 지정해 [ToStringCreator]를 생성하고 본문을 적용합니다.
 *
 * ## 동작/계약
 * - 전달받은 [valueStyler]로 [ToStringCreator]를 생성합니다.
 * - [body]는 생성된 [ToStringCreator]에 바로 적용됩니다.
 *
 * ```kotlin
 * val creator = toStringCreatorOf(Any(), DefaultValueStyler()) {
 *     append("name", "debop")
 * }
 * // creator.toString().contains("name") == true
 * ```
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    valueStyler: ValueStyler = DefaultValueStyler(),
    body: ToStringCreator.() -> Unit,
): ToStringCreator {
    return ToStringCreator(obj, valueStyler).apply(body)
}

/**
 * [ToStringStyler]를 지정해 [ToStringCreator]를 생성하고 본문을 적용합니다.
 *
 * ## 동작/계약
 * - 전달한 [styler]로 [ToStringCreator]를 생성합니다.
 * - 반환된 객체의 `toString()`은 지정한 스타일 규칙을 따릅니다.
 *
 * ```kotlin
 * val creator = toStringCreatorOf(Any(), DefaultToStringStyler(DefaultValueStyler())) {
 *     append("id", 1)
 * }
 * // creator.toString().contains("id = 1") == true
 * ```
 *
 * @see [io.bluetape4k.ToStringBuilder]
 */
inline fun toStringCreatorOf(
    obj: Any,
    styler: ToStringStyler = DefaultToStringStyler(DefaultValueStyler()),
    body: ToStringCreator.() -> Unit,
): ToStringCreator =
    ToStringCreator(obj, styler).apply(body)

/**
 * `tokens[field] = value` 문법으로 [ToStringCreator.append]를 호출하는 헬퍼입니다.
 *
 * ## 동작/계약
 * - 각 `set` 연산자는 타입별 [ToStringCreator.append] 오버로드를 호출합니다.
 * - 반환값은 내부 [ToStringCreator]이며 연쇄 호출에 사용할 수 있습니다.
 *
 * ```kotlin
 * val creator = ToStringCreator(Any())
 * val tokens = ToStringCreatorAppendTokens(creator)
 * tokens["name"] = "debop"
 * // creator.toString().contains("name") == true
 * ```
 */
class ToStringCreatorAppendTokens(private val creator: ToStringCreator) {

    /**
     * Boolean 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * val creator = ToStringCreator(Any())
     * val tokens = ToStringCreatorAppendTokens(creator)
     * tokens["active"] = true
     * // creator.toString().contains("active") == true
     * ```
     */
    operator fun set(fieldName: String, value: Boolean): ToStringCreator = creator.append(fieldName, value)

    /**
     * Byte 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["grade"] = 1.toByte()
     * // creator.toString().contains("grade") == true
     * ```
     */
    operator fun set(fieldName: String, value: Byte): ToStringCreator = creator.append(fieldName, value)

    /**
     * Char 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["initial"] = 'D'
     * // creator.toString().contains("initial") == true
     * ```
     */
    operator fun set(fieldName: String, value: Char): ToStringCreator = creator.append(fieldName, value)

    /**
     * Short 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["count"] = 2.toShort()
     * // creator.toString().contains("count") == true
     * ```
     */
    operator fun set(fieldName: String, value: Short): ToStringCreator = creator.append(fieldName, value)

    /**
     * Int 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["age"] = 42
     * // creator.toString().contains("age = 42") == true
     * ```
     */
    operator fun set(fieldName: String, value: Int): ToStringCreator = creator.append(fieldName, value)

    /**
     * Long 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["version"] = 1L
     * // creator.toString().contains("version") == true
     * ```
     */
    operator fun set(fieldName: String, value: Long): ToStringCreator = creator.append(fieldName, value)

    /**
     * Float 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["ratio"] = 1.5f
     * // creator.toString().contains("ratio") == true
     * ```
     */
    operator fun set(fieldName: String, value: Float): ToStringCreator = creator.append(fieldName, value)

    /**
     * Double 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     *
     * ```kotlin
     * tokens["score"] = 99.9
     * // creator.toString().contains("score") == true
     * ```
     */
    operator fun set(fieldName: String, value: Double): ToStringCreator = creator.append(fieldName, value)

    /**
     * 임의 객체 값을 필드로 추가합니다.
     *
     * ## 동작/계약
     * - `append(fieldName, value)`를 호출한 [ToStringCreator]를 반환합니다.
     * - [value]가 `null`이어도 호출 가능합니다.
     *
     * ```kotlin
     * tokens["name"] = "debop"
     * // creator.toString().contains("name") == true
     * ```
     */
    operator fun set(fieldName: String, value: Any?): ToStringCreator = creator.append(fieldName, value)
}
