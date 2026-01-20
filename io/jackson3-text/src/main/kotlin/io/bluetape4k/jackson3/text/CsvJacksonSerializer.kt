package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.csv.CsvMapper

class CsvJacksonSerializer(
    mapper: CsvMapper = JacksonText.Csv.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
