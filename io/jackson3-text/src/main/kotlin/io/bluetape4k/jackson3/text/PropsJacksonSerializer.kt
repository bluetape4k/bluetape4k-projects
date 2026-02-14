package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.javaprop.JavaPropsMapper

/**
 * Jackson JSON 처리에서 사용하는 `PropsJacksonSerializer` 타입입니다.
 */
class PropsJacksonSerializer(
    mapper: JavaPropsMapper = JacksonText.Props.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
