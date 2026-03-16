package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * YAML 텍스트 포맷을 처리하는 Jackson 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonText.Yaml.defaultMapper]입니다.
 * - 직렬화/역직렬화는 [JacksonSerializer] 구현 경로를 사용합니다.
 * - 입력 객체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val serializer = YamlJacksonSerializer()
 * val text = serializer.serializeAsString(mapOf("name" to "debop"))
 * // text.contains("name") == true
 * ```
 *
 * @param mapper YAML 처리를 위한 Jackson mapper입니다.
 */
class YamlJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Yaml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
