package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import io.bluetape4k.codec.Url62
import java.util.*

/**
 * JsonNode의 UUID 값을 Base62로 인코딩하여 JSON 직렬화합니다.
 *
 * @see [JsonUuidEncoder]
 * @see [JsonUuidBase62Deserializer]
 * @see [Url62]
 */
class JsonUuidBase62Serializer: UUIDSerializer() {

    override fun serialize(value: UUID?, gen: JsonGenerator, provider: SerializerProvider?) {
        value?.run { gen.writeString(Url62.encode(this)) }
    }
}
