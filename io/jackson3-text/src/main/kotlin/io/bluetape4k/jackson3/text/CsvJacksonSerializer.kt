package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.csv.CsvMapper

/**
 * CSV(Comma-Separated Values) 형식을 사용하는 Jackson Serializer 구현체입니다.
 * [CsvMapper]를 기반으로 CSV 데이터를 직렬화/역직렬화합니다.
 *
 * @param mapper CSV 데이터 처리를 위한 [CsvMapper] (기본값: [JacksonText.Csv.defaultMapper])
 */
class CsvJacksonSerializer(
    mapper: CsvMapper = JacksonText.Csv.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
