package io.bluetape4k.jackson3.uuid

import io.bluetape4k.codec.Url62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.jdk.UUIDDeserializer
import java.util.*

/**
 * Base62 또는 표준 UUID 문자열을 UUID로 역직렬화하는 deserializer입니다.
 *
 * ## 동작/계약
 * - 문자열 토큰만 처리하며 다른 토큰이면 예외를 발생시킵니다.
 * - 표준 UUID 패턴이면 기본 UUID 역직렬화 경로를 사용합니다.
 * - 표준 패턴이 아니면 Base62로 간주해 [Url62.decode]를 호출합니다.
 *
 * ```kotlin
 * val uuid = Url62.decode("6gVuscij1cec8CelrpHU5h")
 * // uuid.toString().length == 36
 * ```
 *
 * @see [JsonUuidEncoder]
 * @see [JsonUuidBase62Serializer]
 * @see [Url62]
 */
class JsonUuidBase62Deserializer: UUIDDeserializer() {

    companion object: KLogging() {
        private val UUID_PATTERN =
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".toRegex()
    }

    /** 문자열 토큰을 UUID로 변환합니다. */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UUID {
        val token = p.currentToken()
        log.debug { "deserialize token=$token" }

        if (token == JsonToken.VALUE_STRING) {
            val text = p.valueAsString.trim()

            log.debug { "text=$text" }

            return if (looksLikeUuid(text)) {
                super.deserialize(p, ctxt)
            } else {
                Url62.decode(text)
            }
        }
        error("This is not uuid or url62 encoded id. name=${p.currentName()}, value=${p.currentValue()}")
    }

    private fun looksLikeUuid(value: String): Boolean = UUID_PATTERN.matches(value)
}
