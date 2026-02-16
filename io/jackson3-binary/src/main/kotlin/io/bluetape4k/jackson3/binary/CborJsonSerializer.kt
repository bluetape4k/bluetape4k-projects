package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.cbor.CBORMapper

/**
 * Binary JSON 직렬화를 위한 CBOR Serializer
 *
 * 이 클래스는 더 이상 사용되지 않습니다. [CborJacksonSerializer]를 대신 사용하세요.
 *
 * ```
 * val serializer = CborJsonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [CBORMapper] 인스턴스
 */
@Deprecated("use CborJacksonSerializer", replaceWith = ReplaceWith("CborJacksonSerializer"))
class CborJsonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
