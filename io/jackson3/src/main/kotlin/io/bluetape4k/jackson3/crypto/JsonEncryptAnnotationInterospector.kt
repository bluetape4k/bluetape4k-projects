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
 * [JsonEncrypt] 애너테이션을 읽어 암복호화 serializer/deserializer를 선택하는 인트로스펙터입니다.
 *
 * ## 동작/계약
 * - 애너테이션이 존재할 때만 커스텀 serializer/deserializer를 반환합니다.
 * - encryptor 타입별 인스턴스를 캐시해 재사용합니다.
 *
 * ```kotlin
 * val introspector = JsonEncryptAnnotationIntrospector()
 * // @JsonEncrypt 필드에 대해 serializer/deserializer를 선택함
 * ```
 *
 * @see [JsonEncrypt]
 * @see [JsonEncryptSerializer]
 * @see [JsonEncryptDeserializer]
 * @see [JsonEncryptModule]
 */
class JsonEncryptAnnotationIntrospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonEncrypt::class.java
        private val serializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptSerializer>()
        private val deserializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptDeserializer>()
    }

    /** 직렬화기 선택 규칙을 반환합니다. */
    override fun findSerializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find serializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let { ann ->
            serializers.computeIfAbsent(ann.encryptor) {
                JsonEncryptSerializer(ann)
            }
        }
    }

    /** 역직렬화기 선택 규칙을 반환합니다. */
    override fun findDeserializer(config: MapperConfig<*>, a: Annotated): Any? {
        val jsonEncrypt = _findAnnotation(a, ANNOTATION_TYPE)
        log.debug { "find deserializer. annotated=$a, jsonEncrypt=$jsonEncrypt" }

        return jsonEncrypt?.let { ann ->
            deserializers.computeIfAbsent(ann.encryptor) {
                JsonEncryptDeserializer(ann)
            }
        }
    }
}

/** "Interospector" 오타를 수정한 [JsonEncryptAnnotationIntrospector]의 하위 호환 별칭입니다. */
@Deprecated(
    "오타가 수정된 JsonEncryptAnnotationIntrospector 를 사용하세요.",
    ReplaceWith("JsonEncryptAnnotationIntrospector")
)
typealias JsonEncryptAnnotationInterospector = JsonEncryptAnnotationIntrospector
