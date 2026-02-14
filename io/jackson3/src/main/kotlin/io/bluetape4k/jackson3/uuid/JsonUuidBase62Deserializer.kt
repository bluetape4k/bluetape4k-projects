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
 * JsonNode의 Base62로 인코딩된 UUID 값을 UUID로 디코딩하여 JSON 역직렬화합니다.
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

    /**
     * Jackson JSON 처리에서 데이터를 역직렬화하는 `deserialize` 함수를 제공합니다.
     */
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
