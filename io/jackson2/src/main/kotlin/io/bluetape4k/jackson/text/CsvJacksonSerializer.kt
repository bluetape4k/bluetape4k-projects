package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * CSV 텍스트 포맷을 처리하는 Jackson 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonText.Csv.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 [JacksonSerializer] 구현을 따릅니다.
 * - 입력 객체를 변경하지 않고 결과 문자열/객체를 새로 생성합니다.
 *
 * ```kotlin
 * val serializer = CsvJacksonSerializer()
 * val text = serializer.serializeAsString(mapOf("id" to 1))
 * // text.isNotBlank() == true
 * ```
 *
 * @param mapper CSV 처리를 위한 Jackson mapper입니다.
 */
class CsvJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Csv.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
