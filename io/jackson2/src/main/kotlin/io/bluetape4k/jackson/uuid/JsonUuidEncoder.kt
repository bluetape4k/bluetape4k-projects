package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * UUID 필드의 JSON 인코딩 방식을 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonUuidModule]이 등록된 매퍼에서만 동작합니다.
 * - 기본값은 [JsonUuidEncoderType.BASE62]이며, 지정하지 않으면 Base62 문자열로 처리됩니다.
 * - 필드 값 자체는 변경하지 않고 직렬화/역직렬화 표현만 바뀝니다.
 *
 * ```kotlin
 * data class User(
 *     @field:JsonUuidEncoder
 *     val userId: UUID,
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
annotation class JsonUuidEncoder(
    val value: JsonUuidEncoderType = JsonUuidEncoderType.BASE62,
)
