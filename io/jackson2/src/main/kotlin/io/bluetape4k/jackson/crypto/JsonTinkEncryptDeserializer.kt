package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.safeLet
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonTinkEncrypt] 애너테이션이 붙은 문자열 필드를 Tink로 복호화해 역직렬화하는 deserializer입니다.
 *
 * ## 동작/계약
 * - 필드의 [TinkEncryptAlgorithm]별 deserializer 인스턴스를 캐시합니다.
 * - 애너테이션이 있으면 암호문 문자열을 읽어 복호화 값을 반환합니다.
 * - 애너테이션이 없으면 null을 반환해 기본 역직렬화 경로에 위임됩니다.
 *
 * ```kotlin
 * val deserializer = JsonTinkEncryptDeserializer()
 * // @JsonTinkEncrypt 필드는 Tink로 복호화된 평문 문자열로 복원됨
 * ```
 *
 * @property annotation 필드에 선언된 [JsonTinkEncrypt] 애너테이션
 *
 * @see JsonTinkEncrypt
 */
class JsonTinkEncryptDeserializer(
    private val annotation: JsonTinkEncrypt? = null,
): StdDeserializer<String>(String::class.java), ContextualDeserializer {

    companion object: KLogging() {
        private val defaultDeserializer = JsonTinkEncryptDeserializer()
        private val deserializers = ConcurrentHashMap<TinkEncryptAlgorithm, JsonTinkEncryptDeserializer>()
    }

    /** 필드 애너테이션에 맞는 deserializer 인스턴스를 선택합니다. */
    override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty?): JsonDeserializer<*> {
        val annotation = property?.getAnnotation(JsonTinkEncrypt::class.java)

        return when (annotation) {
            null -> defaultDeserializer
            else -> deserializers.getOrPut(annotation.algorithm) {
                JsonTinkEncryptDeserializer(annotation).apply {
                    log.debug { "Create JsonTinkEncryptDeserializer ... ${annotation.algorithm}" }
                }
            }
        }
    }

    /** 암호문 문자열을 Tink로 복호화해 평문 문자열로 반환합니다. */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): String? {
        return safeLet(annotation, p) { ann, parser ->
            val encryptedText = parser.codec.readValue(parser, String::class.java)
            ann.algorithm.getEncryptor().decrypt(encryptedText)
        }
    }
}
