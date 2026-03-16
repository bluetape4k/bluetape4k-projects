package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * 이전 Smile 직렬화기 이름을 유지하기 위한 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 실제 직렬화/역직렬화는 [SmileJacksonSerializer]와 동일 경로를 사용합니다.
 * - 신규 코드는 [SmileJacksonSerializer] 사용을 권장합니다.
 * - 기본 [mapper]는 [JacksonBinary.Smile.defaultMapper]입니다.
 *
 * ```kotlin
 * val serializer = SmileJsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Smile 처리에 사용할 Jackson mapper입니다.
 */
@Deprecated("use SmileJacksonSerializer", replaceWith = ReplaceWith("SmileJacksonSerializer"))
class SmileJsonSerializer(
    mapper: SmileMapper = JacksonBinary.Smile.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
