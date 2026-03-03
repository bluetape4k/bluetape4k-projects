package io.bluetape4k.jackson3.uuid

import io.bluetape4k.logging.KLogging
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.deser.jdk.UUIDDeserializer
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.AnnotatedMethod
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import tools.jackson.databind.ser.jdk.UUIDSerializer
import java.util.*

/**
 * [JsonUuidEncoder] 애너테이션을 읽어 UUID 직렬화기/역직렬화기를 선택하는 인트로스펙터입니다.
 *
 * ## 동작/계약
 * - UUID 타입일 때만 커스텀 serializer/deserializer를 반환합니다.
 * - 애너테이션이 없으면 기본 UUID serializer/deserializer를 사용합니다.
 * - `BASE62`는 Base62 변환기, `PLAIN`은 표준 UUID 변환기를 사용합니다.
 *
 * ```kotlin
 * val introspector = JsonUuidEncoderAnnotationIntrospector()
 * // @JsonUuidEncoder(BASE62) 필드에 Base62 serializer 선택
 * ```
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidEncoderAnnotationIntrospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE: Class<JsonUuidEncoder> = JsonUuidEncoder::class.java

        private val base62Serializer = JsonUuidBase62Serializer()
        private val base62Deserializer = JsonUuidBase62Deserializer()

        private val uuidSerializer = UUIDSerializer()
        private val uuidDeserializer = UUIDDeserializer()
    }

    /** 직렬화기 선택 규칙을 반환합니다. */
    override fun findSerializer(config: MapperConfig<*>, a: Annotated): Any? {
        val annotation = _findAnnotation(a, ANNOTATION_TYPE)

        if (a.rawType == UUID::class.java) {
            return annotation?.let {
                when (it.value) {
                    JsonUuidEncoderType.BASE62 -> base62Serializer
                    JsonUuidEncoderType.PLAIN -> uuidSerializer
                }
            } ?: uuidSerializer
        }
        return null
    }

    /** 역직렬화기 선택 규칙을 반환합니다. */
    override fun findDeserializer(config: MapperConfig<*>, a: Annotated): Any? {
        val annotation = _findAnnotation(a, ANNOTATION_TYPE)

        if (rawDeserializationType(a) == UUID::class.java) {
            return annotation?.let {
                when (it.value) {
                    JsonUuidEncoderType.BASE62 -> base62Deserializer
                    JsonUuidEncoderType.PLAIN -> uuidDeserializer
                }
            } ?: uuidDeserializer
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
