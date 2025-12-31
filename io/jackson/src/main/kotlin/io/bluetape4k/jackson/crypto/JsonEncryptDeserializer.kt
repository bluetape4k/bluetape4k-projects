package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.safeLet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt] annotation이 적용된 필드의 암호화된 값을 JSON 역직렬화 시에 복호화를 수행합니다.
 *
 * @property annotation [JsonEncrypt] annotation or null
 *
 * @see JsonEncrypt
 */
class JsonEncryptDeserializer(
    private val annotation: JsonEncrypt? = null,
): StdDeserializer<String>(String::class.java), ContextualDeserializer {

    companion object: KLogging() {
        private val defaultDeserializer = JsonEncryptDeserializer()
        private val deserializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptDeserializer>()
    }

    override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty?): JsonDeserializer<*> {
        val annotation = property?.getAnnotation(JsonEncrypt::class.java)

        return when (annotation) {
            null -> defaultDeserializer
            else -> deserializers.computeIfAbsent(annotation.encryptor) {
                JsonEncryptDeserializer(annotation).apply {
                    log.debug { "Create JsonEncryptDeserializer ...${annotation.encryptor}" }
                }
            }
        }
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): String? {
        return safeLet(annotation, p) { ann, parser ->
            val codec = parser.codec
            val encryptedText = codec.readValue(parser, String::class.java)
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            encryptor.decrypt(encryptedText)
        }
    }
}
