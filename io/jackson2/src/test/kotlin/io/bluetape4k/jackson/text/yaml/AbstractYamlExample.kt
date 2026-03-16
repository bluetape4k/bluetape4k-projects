package io.bluetape4k.jackson.text.yaml

import io.bluetape4k.jackson.text.AbstractJacksonTextTest
import io.bluetape4k.jackson.text.JacksonText
import io.bluetape4k.logging.KLogging

abstract class AbstractYamlExample: AbstractJacksonTextTest() {

    companion object: KLogging()

    protected val yamlMapper by lazy { JacksonText.Yaml.defaultMapper }
    protected val yamlFactory by lazy { JacksonText.Yaml.defaultFactory }
    protected val jsonMapper by lazy { JacksonText.Yaml.defaultJsonMapper }
}
