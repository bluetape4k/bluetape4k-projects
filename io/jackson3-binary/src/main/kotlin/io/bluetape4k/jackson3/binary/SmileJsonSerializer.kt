package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.smile.SmileMapper

/**
 * 이전 Smile 직렬화기 이름을 유지하는 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 실제 직렬화 경로는 [SmileJacksonSerializer]와 동일합니다.
 * - 신규 코드는 [SmileJacksonSerializer] 사용을 권장합니다.
 * - [mapper] 기본값은 [JacksonBinary.Smile.defaultMapper]입니다.
 *
 * ```kotlin
 * val serializer = SmileJsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Smile 처리를 위한 Jackson mapper입니다.
 */
@Deprecated("use SmileJacksonSerializer", replaceWith = ReplaceWith("SmileJacksonSerializer"))
class SmileJsonSerializer(
    mapper: SmileMapper = JacksonBinary.Smile.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
