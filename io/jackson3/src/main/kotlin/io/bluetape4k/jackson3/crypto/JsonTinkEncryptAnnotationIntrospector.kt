package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonTinkEncrypt] 애너테이션을 읽어 Tink 기반 암복호화 serializer/deserializer를 선택하는 인트로스펙터입니다.
 *
 * ## 동작/계약
 * - 애너테이션이 존재할 때만 커스텀 serializer/deserializer를 반환합니다.
 * - [TinkEncryptAlgorithm]별 인스턴스를 캐시해 재사용합니다.
 *
 * ```kotlin
 * val introspector = JsonTinkEncryptAnnotationIntrospector()
 * // @JsonTinkEncrypt 필드에 대해 Tink serializer/deserializer를 선택함
 * ```
 *
 * @see [JsonTinkEncrypt]
 * @see [JsonTinkEncryptSerializer]
 * @see [JsonTinkEncryptDeserializer]
 * @see [JsonTinkEncryptModule]
 */
class JsonTinkEncryptAnnotationIntrospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonTinkEncrypt::class.java
        private val serializers = ConcurrentHashMap<TinkEncryptAlgorithm, JsonTinkEncryptSerializer>()
        private val deserializers = ConcurrentHashMap<TinkEncryptAlgorithm, JsonTinkEncryptDeserializer>()
    }

    /** Tink 직렬화기 선택 규칙을 반환합니다. */
    override fun findSerializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonTinkEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find serializer. annotated=$a, jsonTinkEncrypt=$jsonTinkEncrypt" }

        return jsonTinkEncrypt?.let { ann ->
            serializers.computeIfAbsent(ann.algorithm) {
                JsonTinkEncryptSerializer(ann)
            }
        }
    }

    /** Tink 역직렬화기 선택 규칙을 반환합니다. */
    override fun findDeserializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonTinkEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find deserializer. annotated=$a, jsonTinkEncrypt=$jsonTinkEncrypt" }

        return jsonTinkEncrypt?.let { ann ->
            deserializers.computeIfAbsent(ann.algorithm) {
                JsonTinkEncryptDeserializer(ann)
            }
        }
    }
}
