package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * CBOR 포맷을 사용하는 Jackson 기반 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonBinary.CBOR.defaultMapper]를 사용합니다.
 * - 직렬화/역직렬화 동작은 상위 [JacksonSerializer] 구현을 그대로 따릅니다.
 * - 수신 객체를 변경하지 않고 바이트 배열/객체를 새로 생성합니다.
 *
 * ```kotlin
 * val serializer = CborJacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper CBOR 처리에 사용할 Jackson mapper입니다.
 */
class CborJacksonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
