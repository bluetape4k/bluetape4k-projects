package io.bluetape4k.jackson3.uuid

import io.bluetape4k.codec.Url62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.jdk.UUIDSerializer
import java.util.*

/**
 * UUID 값을 Base62 문자열로 직렬화하는 serializer입니다.
 *
 * ## 동작/계약
 * - 입력 UUID가 null이면 값을 쓰지 않습니다.
 * - null이 아니면 [Url62.encode] 결과를 JSON 문자열로 기록합니다.
 *
 * ```kotlin
 * val text = Url62.encode(UUID.fromString("413684f2-e4db-46a1-8ac7-e7225cebbfd3"))
 * // text.isNotBlank() == true
 * ```
 *
 * @see [JsonUuidEncoder]
 * @see [JsonUuidBase62Deserializer]
 * @see [Url62]
 */
class JsonUuidBase62Serializer: UUIDSerializer() {

    companion object: KLogging()

    /** UUID를 Base62 문자열로 변환해 기록합니다. */
    override fun serialize(value: UUID?, gen: JsonGenerator, context: SerializationContext) {
        value?.run {
            val encoded = Url62.encode(this)
            log.debug { "serialize value=$value, encoded=$encoded" }
            gen.writeString(encoded)
        }
    }
}
