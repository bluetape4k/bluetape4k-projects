package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Jackson JSON 처리에서 사용하는 `YamlJacksonSerializer` 타입입니다.
 */
class YamlJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Yaml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
