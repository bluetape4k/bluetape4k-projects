package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.smile.SmileMapper

/**
 * Binary JSON 직렬화를 위한 Smile Serializer
 *
 * ```
 * val serializer = SmileJacksonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [SmileMapper] 인스턴스
 */
class SmileJacksonSerializer(
    mapper: SmileMapper = JacksonBinary.Smile.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
