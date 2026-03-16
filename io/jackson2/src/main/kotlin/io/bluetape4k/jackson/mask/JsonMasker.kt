package io.bluetape4k.jackson.mask

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * 문자열 필드를 고정 마스킹 문자열로 직렬화하도록 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonMaskerSerializer]가 적용되어 원본 값 대신 [value]를 출력합니다.
 * - 역직렬화에는 영향을 주지 않으며 직렬화 표현만 변경합니다.
 *
 * ```kotlin
 * data class User(
 *     val name: String,
 *     @field:JsonMasker("masked")
 *     val mobile: String
 * )
 * // 직렬화 시 mobile 필드는 "masked"로 출력됨
 * // {"name":"debop","mobile":"masked"}
 * ```
 *
 * @property value 필드 값을 대체할 마스킹 문자열
 */
@JacksonAnnotationsInside
@JsonSerialize(using = JsonMaskerSerializer::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
annotation class JsonMasker(
    val value: String = DEFAULT_MASKED_STRING,
) {
    companion object {
        const val DEFAULT_MASKED_STRING = "__masked__"
    }
}
