package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

/**
 * [JsonMasker]가 적용된 필드를 마스킹 문자열로 직렬화하는 serializer입니다.
 *
 * ## 동작/계약
 * - [jsonMasker]가 있으면 원본 값 대신 마스킹 문자열을 기록합니다.
 * - [jsonMasker]가 없으면 원본 값을 그대로 기록합니다.
 *
 * ```kotlin
 * val serializer = JsonMaskerSerializer(JsonMasker("__masked__"))
 * // 직렬화 값이 "__masked__"로 대체됨
 * ```
 *
 * @property jsonMasker 필드에 선언된 JsonMasker 애너테이션
 *
 * @see [JsonMasker]
 */
class JsonMaskerSerializer(private val jsonMasker: JsonMasker? = null): StdSerializer<Any>(Any::class.java) {

    companion object: KLogging()

    /** 마스킹 규칙에 따라 값을 기록합니다. */
    override fun serialize(value: Any?, gen: JsonGenerator, context: SerializationContext) {
        when (jsonMasker) {
            null -> gen.writeString(value.toString())
            else -> gen.writeString(jsonMasker.value)
        }
    }
}
