package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import io.bluetape4k.codec.Url62
import io.bluetape4k.logging.KLogging
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

    /** JSON 문자열에서 UUID를 역직렬화합니다. 표준 UUID 형식이면 그대로, 아니면 Base62 디코딩합니다. */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UUID {
        val token = p.currentToken

        if (token == JsonToken.VALUE_STRING) {
            val text = p.valueAsString.trim()

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
