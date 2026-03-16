package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.csv.CsvMapper

/**
 * CSV 텍스트 포맷을 처리하는 Jackson 3 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonText.Csv.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 [JacksonSerializer] 구현을 따릅니다.
 * - 입력 객체를 mutate 하지 않습니다.
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
    mapper: CsvMapper = JacksonText.Csv.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
