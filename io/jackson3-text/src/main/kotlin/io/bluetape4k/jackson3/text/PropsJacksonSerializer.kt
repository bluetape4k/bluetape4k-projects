package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.javaprop.JavaPropsMapper

/**
 * Java Properties 형식을 사용하는 Jackson Serializer 구현체입니다.
 * [JavaPropsMapper]를 기반으로 Properties 데이터를 직렬화/역직렬화합니다.
 *
 * @param mapper Java Properties 데이터 처리를 위한 [JavaPropsMapper] (기본값: [JacksonText.Props.defaultMapper])
 */
class PropsJacksonSerializer(
    mapper: JavaPropsMapper = JacksonText.Props.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
