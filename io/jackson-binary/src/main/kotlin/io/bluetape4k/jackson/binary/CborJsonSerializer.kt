package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

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
