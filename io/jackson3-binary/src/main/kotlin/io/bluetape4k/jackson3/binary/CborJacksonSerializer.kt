package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.cbor.CBORMapper

/**
 * CBOR(Concise Binary Object Representation) 바이너리 포맷을 사용하는 Jackson Serializer 구현체입니다.
 *
 * CBOR는 JSON과 호환되는 바이너리 포맷으로, JSON 대비 작은 크기와 빠른 파싱 속도를 제공합니다.
 *
 * ```
 * val serializer = CborJacksonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [CBORMapper] 인스턴스
 */
class CborJacksonSerializer(
    mapper: CBORMapper = JacksonBinary.CBOR.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
