package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.yaml.YAMLMapper

class YamlJacksonSerializer(
    mapper: YAMLMapper = JacksonText.Yaml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
