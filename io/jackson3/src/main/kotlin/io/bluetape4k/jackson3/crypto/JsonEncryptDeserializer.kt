package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.safeLet
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

/**
 * [JsonEncrypt] annotation이 적용된 필드의 암호화된 값을 JSON 역직렬화 시에 복호화를 수행합니다.
 *
 * @property annotation [JsonEncrypt] annotation or null
 *
 * @see JsonEncrypt
 */
class JsonEncryptDeserializer(
    private val annotation: JsonEncrypt? = null,
): StdDeserializer<String>(String::class.java) {

    companion object: KLogging()

    /**
     * JSON에서 암호화된 문자열을 읽어 복호화하여 반환합니다.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): String? {
        return safeLet(annotation, p) { ann, parser ->
            val readContext = parser.objectReadContext()
            val encryptedText = readContext.readValue(parser, String::class.java)
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            encryptor.decrypt(encryptedText)
        }
    }
}
