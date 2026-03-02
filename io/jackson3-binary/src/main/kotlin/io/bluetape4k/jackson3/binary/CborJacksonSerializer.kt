package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.cbor.CBORMapper

/**
 * CBOR 포맷을 사용하는 Jackson 3 기반 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonBinary.CBOR.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 상위 [JacksonSerializer] 구현을 따릅니다.
 * - 입력 객체를 변경하지 않고 결과를 새로 생성합니다.
 *
 * ```kotlin
 * val serializer = CborJacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper CBOR 처리를 위한 Jackson mapper입니다.
 */
class CborJacksonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
