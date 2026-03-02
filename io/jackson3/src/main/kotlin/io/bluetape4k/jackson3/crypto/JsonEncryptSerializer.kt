package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.safeLet
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

/**
 * [JsonEncrypt]가 적용된 문자열 필드를 암호화해 직렬화하는 serializer입니다.
 *
 * ## 동작/계약
 * - [annotation]과 값이 모두 있으면 암호화 문자열을 기록합니다.
 * - [annotation]이 없거나 값이 null이면 원본 값을 그대로 기록합니다.
 *
 * ```kotlin
 * val serializer = JsonEncryptSerializer()
 * // @JsonEncrypt 필드는 암호문 문자열로 직렬화됨
 * ```
 *
 * @property annotation 필드에 선언된 JsonEncrypt 애너테이션
 *
 * @see JsonEncrypt
 */
class JsonEncryptSerializer(
    private val annotation: JsonEncrypt? = null,
): StdSerializer<String>(String::class.java) {

    companion object: KLogging()

    /** 문자열 값을 암호화 규칙에 따라 기록합니다. */
    override fun serialize(value: String?, gen: JsonGenerator, context: SerializationContext) {
        safeLet(annotation, value) { ann, v ->
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            val encryptedText = encryptor.encrypt(v)
            gen.writeString(encryptedText)
        } ?: gen.writeRaw(value)
    }
}
