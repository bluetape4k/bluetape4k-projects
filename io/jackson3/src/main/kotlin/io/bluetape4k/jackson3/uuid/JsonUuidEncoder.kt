package io.bluetape4k.jackson3.uuid

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * UUID 필드의 JSON 인코딩 방식을 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonUuidModule] 등록 시 애너테이션 인트로스펙터가 serializer/deserializer를 선택합니다.
 * - 기본값은 [JsonUuidEncoderType.BASE62]입니다.
 *
 * ```kotlin
 * data class User(
 *     @field:JsonUuidEncoder val userId: UUID,
 *     @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN) val plainId: UUID,
 * )
 * // userId는 Base62, plainId는 표준 UUID 문자열로 직렬화됨
 * ```
 *
 * @property value UUID 인코딩 방식
 *
 * @see [JsonUuidEncoderType]
 * @see [JsonUuidBase62Serializer]
 * @see [JsonUuidBase62Deserializer]
 */
@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
annotation class JsonUuidEncoder(
    val value: JsonUuidEncoderType = JsonUuidEncoderType.BASE62,
)
