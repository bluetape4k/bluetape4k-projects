package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.safeLet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt] annotation이 적용된 속성의 정보를 JSON 직렬화 시에 암호화하여 문자열로 씁니다.
 *
 * @property annotation [JsonEncrypt] annotation or null
 *
 * @see JsonEncrypt
 */
class JsonEncryptSerializer(
    private val annotation: JsonEncrypt? = null,
): StdSerializer<String>(String::class.java), ContextualSerializer {

    companion object: KLogging() {
        private val defaultSerializer = JsonEncryptSerializer()
        private val serializers: MutableMap<KClass<out Encryptor>, JsonEncryptSerializer> = ConcurrentHashMap()
    }

    override fun createContextual(prov: SerializerProvider?, property: BeanProperty?): JsonSerializer<*> {
        val annotation = property?.getAnnotation(JsonEncrypt::class.java)

        return when (annotation) {
            null -> defaultSerializer
            else -> serializers.getOrPut(annotation.encryptor) {
                JsonEncryptSerializer(annotation).apply {
                    log.debug { "create JsonEncryptSerializer ... ${annotation.encryptor}" }
                }
            }
        }
    }

    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider?) {
        safeLet(annotation, value) { ann, v ->
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            val encryptedText = encryptor.encrypt(v)
            gen.writeString(encryptedText)
        } ?: gen.writeString(value)
    }
}
