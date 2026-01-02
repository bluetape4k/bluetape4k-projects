package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector

class JsonEncryptAnnotationInterospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonEncrypt::class.java
    }

    override fun findSerializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find serializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let { JsonEncryptSerializer(it) }
    }

    override fun findDeserializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find deserializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let { JsonEncryptDeserializer(it) }
    }
}
