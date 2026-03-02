package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Smile 포맷을 사용하는 Jackson 기반 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonBinary.Smile.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 상위 [JacksonSerializer]를 따릅니다.
 * - 입력 객체를 변경하지 않고 바이트/객체를 새로 만듭니다.
 *
 * ```kotlin
 * val serializer = SmileJacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Smile 처리에 사용할 Jackson mapper입니다.
 */
class SmileJacksonSerializer(
    mapper: SmileMapper = JacksonBinary.Smile.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
