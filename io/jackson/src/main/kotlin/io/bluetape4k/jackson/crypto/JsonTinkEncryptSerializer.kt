package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.safeLet
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonTinkEncrypt] 애너테이션이 붙은 문자열 필드를 Tink로 암호화해 직렬화하는 serializer입니다.
 *
 * ## 동작/계약
 * - 필드의 [TinkEncryptAlgorithm]별 serializer 인스턴스를 캐시합니다.
 * - 애너테이션과 값이 모두 있을 때만 암호화 문자열을 기록합니다.
 * - 애너테이션이 없거나 값이 null이면 원본 값을 그대로 기록합니다.
 *
 * ```kotlin
 * val serializer = JsonTinkEncryptSerializer()
 * // @JsonTinkEncrypt 필드는 Tink 암호문 문자열로 직렬화됨
 * ```
 *
 * @property annotation 필드에 선언된 [JsonTinkEncrypt] 애너테이션
 *
 * @see JsonTinkEncrypt
 */
class JsonTinkEncryptSerializer(
    private val annotation: JsonTinkEncrypt? = null,
): StdSerializer<String>(String::class.java), ContextualSerializer {

    companion object: KLogging() {
        private val defaultSerializer = JsonTinkEncryptSerializer()
        private val serializers = ConcurrentHashMap<TinkEncryptAlgorithm, JsonTinkEncryptSerializer>()
    }

    /** 필드 애너테이션에 맞는 serializer 인스턴스를 선택합니다. */
    override fun createContextual(prov: SerializerProvider?, property: BeanProperty?): JsonSerializer<*> {
        val annotation = property?.getAnnotation(JsonTinkEncrypt::class.java)

        return when (annotation) {
            null -> defaultSerializer
            else -> serializers.getOrPut(annotation.algorithm) {
                JsonTinkEncryptSerializer(annotation).apply {
                    log.debug { "create JsonTinkEncryptSerializer ... ${annotation.algorithm}" }
                }
            }
        }
    }

    /** 문자열 값을 Tink로 암호화해 기록합니다. */
    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider?) {
        safeLet(annotation, value) { ann, v ->
            val encryptedText = ann.algorithm.getEncryptor().encrypt(v)
            gen.writeString(encryptedText)
        } ?: gen.writeString(value)
    }
}
