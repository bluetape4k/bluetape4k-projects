package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * TOML 형식을 사용하는 Jackson Serializer 구현체입니다.
 * [TomlMapper]를 기반으로 TOML 데이터를 직렬화/역직렬화합니다.
 *
 * @param mapper TOML 데이터 처리를 위한 [ObjectMapper] (기본값: [JacksonText.Toml.defaultMapper])
 */
class TomlJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
