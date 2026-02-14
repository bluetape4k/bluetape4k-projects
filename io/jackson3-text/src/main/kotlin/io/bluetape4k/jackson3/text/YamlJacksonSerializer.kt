package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.yaml.YAMLMapper

/**
 * Jackson JSON 처리에서 사용하는 `YamlJacksonSerializer` 타입입니다.
 */
class YamlJacksonSerializer(
    mapper: YAMLMapper = JacksonText.Yaml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
