package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Jackson JSON 처리에서 사용하는 `TomlJacksonSerializer` 타입입니다.
 */
class TomlJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
