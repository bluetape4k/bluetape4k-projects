package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonMasker] 가 적용된 필드의 정보를 masking 하는 Jackson 용 Serializer 입니다.
 *
 * @property annotation [JsonMasker] annotation or null
 *
 * @see [JsonMasker]
 */
class JsonMaskerSerializer(
    private val annotation: JsonMasker? = null,
): StdSerializer<Any>(Any::class.java) {

    companion object: KLogging() {
        private val defaultSerializer = JsonMaskerSerializer()
        private val serializers: MutableMap<String, JsonMaskerSerializer> = ConcurrentHashMap()
    }

    override fun serialize(value: Any?, gen: JsonGenerator, provider: SerializationContext) {
        when {
            annotation != null -> gen.writeString(annotation.value)
            else -> gen.writeRawValue(value.toString())
        }
    }
}
