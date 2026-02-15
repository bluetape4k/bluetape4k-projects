package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

/**
 * [JsonMasker] 가 적용된 필드의 정보를 masking 하는 Jackson 용 Serializer 입니다.
 *
 * @property jsonMasker [JsonMasker] annotation or null
 *
 * @see [JsonMasker]
 */
class JsonMaskerSerializer(private val jsonMasker: JsonMasker? = null): StdSerializer<Any>(Any::class.java) {

    companion object: KLogging()

    /**
     * [JsonMasker] 어노테이션이 적용된 값을 마스킹 문자열로 대체하여 JSON에 씁니다.
     */
    override fun serialize(value: Any?, gen: JsonGenerator, context: SerializationContext) {
        when (jsonMasker) {
            null -> gen.writeRaw(value.toString())
            else -> gen.writeString(jsonMasker.value)
        }
    }
}
