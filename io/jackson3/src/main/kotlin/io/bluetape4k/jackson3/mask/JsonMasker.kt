package io.bluetape4k.jackson3.mask

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * 문자열 필드를 고정 마스킹 문자열로 직렬화하도록 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonMaskerModule] 등록 시 애너테이션 인트로스펙터가 serializer를 선택합니다.
 * - 역직렬화에는 영향을 주지 않으며 직렬화 표현만 변경합니다.
 *
 * ```kotlin
 * data class User(
 *     val name: String,
 *     @field:JsonMasker("masked")
 *     val mobile: String
 * )
 * // {"name":"debop","mobile":"masked"}
 * ```
 *
 * @property value 필드 값을 대체할 마스킹 문자열
 */
@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
annotation class JsonMasker(
    val value: String = DEFAULT_MASKED_STRING,
) {
    companion object {
        const val DEFAULT_MASKED_STRING = "__masked__"
    }
}
