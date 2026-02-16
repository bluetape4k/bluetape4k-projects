package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Binary JSON 직렬화를 위한 Smile Serializer
 *
 * 이 클래스는 더 이상 사용되지 않습니다. [SmileJacksonSerializer]를 대신 사용하세요.
 *
 * ```
 * val serializer = SmileJsonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [SmileMapper] 인스턴스
 */
@Deprecated("use SmileJacksonSerializer", replaceWith = ReplaceWith("SmileJacksonSerializer"))
class SmileJsonSerializer(
    mapper: SmileMapper = JacksonBinary.Smile.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
