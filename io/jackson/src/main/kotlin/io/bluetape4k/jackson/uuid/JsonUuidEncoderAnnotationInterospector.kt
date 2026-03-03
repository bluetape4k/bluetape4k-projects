package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * [JsonUuidEncoder] 애너테이션을 읽어 UUID 직렬화기/역직렬화기를 선택하는 인트로스펙터입니다.
 *
 * ## 동작/계약
 * - 대상 타입이 UUID일 때만 커스텀 serializer/deserializer를 반환합니다.
 * - 애너테이션이 없으면 Jackson 기본 UUID serializer/deserializer를 사용합니다.
 * - `BASE62` 선택 시 Base62 변환기를, `PLAIN` 선택 시 표준 UUID 변환기를 사용합니다.
 *
 * ```kotlin
 * val introspector = JsonUuidEncoderAnnotationIntrospector()
 * // UUID 필드 + @JsonUuidEncoder(BASE62) -> JsonUuidBase62Serializer 선택
 * ```
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidEncoderAnnotationIntrospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE: Class<JsonUuidEncoder> = JsonUuidEncoder::class.java
    }

    /** UUID 타입 직렬화기 선택 규칙을 반환합니다. */
    override fun findSerializer(annotatedMethod: Annotated): Any? {
        val annotation = _findAnnotation(annotatedMethod, ANNOTATION_TYPE)
        if (annotatedMethod.rawType == UUID::class.java) {
            return annotation?.let {
                when (it.value) {
                    JsonUuidEncoderType.BASE62 -> JsonUuidBase62Serializer::class.java
                    JsonUuidEncoderType.PLAIN -> UUIDSerializer::class.java
                }
            } ?: UUIDSerializer::class.java
        }
        return null
    }

    /** UUID 타입 역직렬화기 선택 규칙을 반환합니다. */
    override fun findDeserializer(annotatedMethod: Annotated): Any? {
        val annotation = _findAnnotation(annotatedMethod, ANNOTATION_TYPE)

        if (rawDeserializationType(annotatedMethod) == UUID::class.java) {
            return annotation?.let {
                when (it.value) {
                    JsonUuidEncoderType.BASE62 -> JsonUuidBase62Deserializer::class.java
                    JsonUuidEncoderType.PLAIN -> UUIDDeserializer::class.java
                }
            } ?: UUIDDeserializer::class.java
        }
        return null
    }

    private fun rawDeserializationType(ann: Annotated): Class<*> {
        return if (ann is AnnotatedMethod && ann.parameterCount == 1) {
            ann.getRawParameterType(0)
        } else {
            ann.rawType
        }
    }
}

/** "Interospector" 오타를 수정한 [JsonUuidEncoderAnnotationIntrospector]의 하위 호환 별칭입니다. */
@Deprecated(
    "오타가 수정된 JsonUuidEncoderAnnotationIntrospector 를 사용하세요.",
    ReplaceWith("JsonUuidEncoderAnnotationIntrospector")
)
typealias JsonUuidEncoderAnnotationInterospector = JsonUuidEncoderAnnotationIntrospector
