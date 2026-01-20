package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.javaprop.JavaPropsMapper

class PropsJacksonSerializer(
    mapper: JavaPropsMapper = JacksonText.Props.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
