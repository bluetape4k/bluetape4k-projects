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
 * [JsonUuidEncoder] annotation이 적용된 필드의 UUID 값을 JSON 직렬화 시에 인코딩하는 Serializer를 관리합니다.
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidEncoderAnnotationInterospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE: Class<JsonUuidEncoder> = JsonUuidEncoder::class.java

        private val base62Serializer = JsonUuidBase62Serializer()
        private val base62Deserializer = JsonUuidBase62Deserializer()

        private val uuidSerializer = UUIDSerializer()
        private val uuidDeserializer = UUIDDeserializer()
    }

    /**
     * UUID 타입 필드에 [JsonUuidEncoder] 어노테이션이 있으면 해당 인코딩 방식의 직렬화기를 반환합니다.
     */
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

    /**
     * UUID 타입 필드에 [JsonUuidEncoder] 어노테이션이 있으면 해당 인코딩 방식의 역직렬화기를 반환합니다.
     */
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
