package io.bluetape4k.jackson3.crypto

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.safeLet
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
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
): StdSerializer<String>(String::class.java) {

    companion object: KLogging() {
        private val defaultSerializer = JsonEncryptSerializer()
        private val serializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptSerializer>()
    }

//    override fun createContextual(prov: SerializerFactory?, property: BeanProperty?): JsonSerializer<*> {
//        val annotation = property?.getAnnotation(JsonEncrypt::class.java)
//
//        return when (annotation) {
//            null -> defaultSerializer
//            else -> serializers.computeIfAbsent(annotation.encryptor) {
//                JsonEncryptSerializer(annotation).apply {
//                    log.debug { "create JsonEncryptSerializer ... ${annotation.encryptor}" }
//                }
//            }
//        }
//    }

    override fun serialize(value: String?, gen: JsonGenerator, context: SerializationContext) {
        safeLet(annotation, value) { ann, v ->
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            val encryptedText = encryptor.encrypt(v)
            gen.writeString(encryptedText)
        } ?: gen.writeString(value)
    }
}
