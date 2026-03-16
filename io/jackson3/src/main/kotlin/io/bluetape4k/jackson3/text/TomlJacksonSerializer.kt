package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.toml.TomlMapper

/**
 * TOML 텍스트 포맷을 처리하는 Jackson 3 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonText.Toml.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 [JacksonSerializer]를 따릅니다.
 * - 입력 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val serializer = TomlJacksonSerializer()
 * val text = serializer.serializeAsString(mapOf("id" to 1))
 * // text.contains("id") == true
 * ```
 *
 * @param mapper TOML 처리를 위한 Jackson mapper입니다.
 */
class TomlJacksonSerializer(
    mapper: TomlMapper = JacksonText.Toml.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
