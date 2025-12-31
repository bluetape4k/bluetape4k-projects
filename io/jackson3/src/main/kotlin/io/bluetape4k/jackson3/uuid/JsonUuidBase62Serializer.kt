package io.bluetape4k.jackson3.uuid

import io.bluetape4k.codec.Url62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.jdk.UUIDSerializer
import java.util.*

/**
 * JsonNode의 UUID 값을 Base62로 인코딩하여 JSON 직렬화합니다.
 *
 * @see [JsonUuidEncoder]
 * @see [JsonUuidBase62Deserializer]
 * @see [Url62]
 */
class JsonUuidBase62Serializer: UUIDSerializer() {

    companion object: KLogging()

    override fun serialize(value: UUID?, gen: JsonGenerator, context: SerializationContext) {
        value?.run {
            val encoded = Url62.encode(this)
            log.debug { "serialize value=$value, encoded=$encoded" }
            gen.writeString(encoded)
        }
    }
}
