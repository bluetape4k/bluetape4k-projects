package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.toml.TomlMapper

/**
 * Jackson JSON 처리에서 사용하는 `TomlJacksonSerializer` 타입입니다.
 */
class TomlJacksonSerializer(
    mapper: TomlMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
