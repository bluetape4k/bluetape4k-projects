package io.bluetape4k.jackson3.crypto

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt] annotation이 적용된 속성의 값을 JSON 직렬화/역직렬화 시에 암호화/복호화하는 Serializer/Deserializer를 관리합니다.
 *
 * @see [JsonEncrypt]
 * @see [JsonEncryptSerializer]
 * @see [JsonEncryptDeserializer]
 * @see [JsonEncryptModule]
 */
class JsonEncryptAnnotationInterospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonEncrypt::class.java
        private val serializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptSerializer>()
        private val deserializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptDeserializer>()
    }

    override fun findSerializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find serializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let {
            serializers.computeIfAbsent(jsonEncrypt.encryptor) {
                JsonEncryptSerializer(jsonEncrypt)
            }
        }
    }

    override fun findDeserializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find deserializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let {
            deserializers.computeIfAbsent(jsonEncrypt.encryptor) {
                JsonEncryptDeserializer(jsonEncrypt)
            }
        }
    }
}
