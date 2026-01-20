package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

class TomlJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
