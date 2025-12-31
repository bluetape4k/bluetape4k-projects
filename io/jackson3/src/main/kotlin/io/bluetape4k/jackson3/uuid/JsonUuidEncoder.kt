package io.bluetape4k.jackson3.uuid

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * 실제 UUID 형식의 Identifier 를 Base62 로 인코딩하거나 단순 문자열로 전달할 수 있도록 합니다.
 *
 * ```
 * data class User(
 *     @field:JsonUuidEncoder
 *     val userId: UUID,
 *     @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)
 *     val plainUserId: UUID,
 *     @field:JsonUuidEncoder(JsonUuidEncoderType.BASE62)
 *     val encodedUserId: UUID,
 *     val username: String,
 * )
 *
 * // JSON 변환 시 다음과 같이 변환됩니다.
 * {
 *     "userId" : "6gVuscij1cec8CelrpHU5h",
 *     "plainUserId" : "413684f2-e4db-46a1-8ac7-e7225cebbfd3",
 *     "encodedUserId" : "6gVuscij1cec8CelrpHU5h",        // base62 encoding for UUID
 *     "username"  : "debop"
 * }
 * ```
 *
 * @property value 인코딩 방식을 지정합니다. (기본값은 [JsonUuidEncoderType.BASE62] 입니다.)
 *
 * @see [JsonUuidEncoderType]
 * @see [JsonUuidBase62Serializer]
 * @see [JsonUuidBase62Deserializer]
 */
@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonUuidEncoder(
    val value: JsonUuidEncoderType = JsonUuidEncoderType.BASE62,
)
