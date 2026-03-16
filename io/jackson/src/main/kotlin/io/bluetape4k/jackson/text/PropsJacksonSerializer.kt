package io.bluetape4k.jackson.text

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Java Properties 텍스트 포맷을 처리하는 Jackson 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonText.Props.defaultMapper]입니다.
 * - 직렬화/역직렬화는 [JacksonSerializer] 구현 경로를 따릅니다.
 * - 입력 객체는 mutate 하지 않습니다.
 *
 * ```kotlin
 * val serializer = PropsJacksonSerializer()
 * val text = serializer.serializeAsString(mapOf("app.name" to "demo"))
 * // text.contains("app.name") == true
 * ```
 *
 * @param mapper Java Properties 처리를 위한 Jackson mapper입니다.
 */
class PropsJacksonSerializer(
    mapper: ObjectMapper = JacksonText.Props.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
