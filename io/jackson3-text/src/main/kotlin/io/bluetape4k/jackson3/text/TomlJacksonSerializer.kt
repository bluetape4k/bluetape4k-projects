package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.toml.TomlMapper

class TomlJacksonSerializer(
    mapper: TomlMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
