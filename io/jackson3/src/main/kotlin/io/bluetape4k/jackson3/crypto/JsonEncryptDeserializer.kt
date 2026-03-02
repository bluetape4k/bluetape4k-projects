package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.safeLet
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

/**
 * [JsonEncrypt]가 적용된 문자열 필드를 복호화해 역직렬화하는 deserializer입니다.
 *
 * ## 동작/계약
 * - [annotation]이 존재하면 암호문 문자열을 읽어 복호화 결과를 반환합니다.
 * - [annotation]이 없으면 null을 반환해 기본 역직렬화 경로로 위임됩니다.
 *
 * ```kotlin
 * val deserializer = JsonEncryptDeserializer()
 * // @JsonEncrypt 필드는 평문 문자열로 복원됨
 * ```
 *
 * @property annotation 필드에 선언된 JsonEncrypt 애너테이션
 *
 * @see JsonEncrypt
 */
class JsonEncryptDeserializer(
    private val annotation: JsonEncrypt? = null,
): StdDeserializer<String>(String::class.java) {

    companion object: KLogging()

    /** 암호문 문자열을 복호화해 반환합니다. */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): String? {
        return safeLet(annotation, p) { ann, parser ->
            val readContext = parser.objectReadContext()
            val encryptedText = readContext.readValue(parser, String::class.java)
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            encryptor.decrypt(encryptedText)
        }
    }
}
