package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.cbor.CBORMapper

/**
 * Binary JSON 직렬화를 위한 CBOR Serializer
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
class CborJsonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
