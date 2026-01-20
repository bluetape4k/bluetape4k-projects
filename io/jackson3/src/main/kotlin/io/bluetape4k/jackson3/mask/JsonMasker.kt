package io.bluetape4k.jackson3.mask

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * Json Property 를 문자열로 변환 시, 민감한 정보를 mask 처리를 수행할 수 있도록 합니다.
 *
 * ```
 * data class User(
 *     val name:String,
 *     @field:JsonMasker("masked")
 *     val mobile: String
 * ): Serializable
 *
 * val user = User("debop", "011-8888-5555")
 * val jsonText = objectMapper.writeAsString(user)
 *
 * // jsonText is { "user": "debop", "mobile": "__masked__" }
 *
 * @property value masking 될 필드의 값을 대체할 문자열
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
