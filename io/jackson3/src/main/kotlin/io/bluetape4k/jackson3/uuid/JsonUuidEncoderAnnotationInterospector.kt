package io.bluetape4k.jackson3.uuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.deser.jdk.UUIDDeserializer
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.AnnotatedMethod
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import tools.jackson.databind.ser.jdk.UUIDSerializer
import java.util.*

/**
 * [JsonUuidEncoder] annotation이 적용된 필드의 UUID 값을 JSON 직렬화 시에 인코딩하는 Serializer를 관리합니다.
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidEncoderAnnotationInterospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE: Class<JsonUuidEncoder> = JsonUuidEncoder::class.java
    }

    override fun findSerializer(config: MapperConfig<*>, annotatedMethod: Annotated): Any? {
        val annotation = _findAnnotation(annotatedMethod, ANNOTATION_TYPE)
        log.debug { "annotatedMethod=$annotatedMethod, annotation=$annotation" }

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


    override fun findDeserializer(config: MapperConfig<*>, annotatedMethod: Annotated): Any? {
        val annotation = _findAnnotation(annotatedMethod, ANNOTATION_TYPE)
        log.debug { "annotatedMethod=$annotatedMethod, annotation=$annotation" }

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
