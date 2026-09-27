package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * 이전 CBOR 직렬화기 이름을 유지하기 위한 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 실제 동작은 [CborJacksonSerializer]와 동일한 [JacksonSerializer] 경로를 사용합니다.
 * - 신규 코드는 [CborJacksonSerializer] 사용을 권장합니다.
 * - 기본 [mapper]는 [JacksonBinary.CBOR.defaultMapper]입니다.
 *
 * ```kotlin
 * val serializer = CborJsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper CBOR 처리에 사용할 Jackson mapper입니다.
 */
@Deprecated("use CborJacksonSerializer", replaceWith = ReplaceWith("CborJacksonSerializer"))
class CborJsonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
