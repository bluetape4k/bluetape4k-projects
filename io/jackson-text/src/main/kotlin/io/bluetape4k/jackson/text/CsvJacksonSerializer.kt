package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * CSV(Comma-Separated Values) 형식을 사용하는 Jackson Serializer 구현체입니다.
 * [CsvMapper]를 기반으로 CSV 데이터를 직렬화/역직렬화합니다.
 *
 * @param mapper CSV 데이터 처리를 위한 [ObjectMapper] (기본값: [JacksonText.Csv.defaultMapper])
 */
class CsvJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Csv.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
