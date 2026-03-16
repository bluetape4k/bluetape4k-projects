package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.cbor.CBORMapper

/**
 * 이전 CBOR 직렬화기 이름을 유지하는 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 실제 직렬화 경로는 [CborJacksonSerializer]와 동일합니다.
 * - 신규 코드는 [CborJacksonSerializer] 사용을 권장합니다.
 * - [mapper] 기본값은 [JacksonBinary.CBOR.defaultMapper]입니다.
 *
 * ```kotlin
 * val serializer = CborJsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper CBOR 처리를 위한 Jackson mapper입니다.
 */
@Deprecated("use CborJacksonSerializer", replaceWith = ReplaceWith("CborJacksonSerializer"))
class CborJsonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
