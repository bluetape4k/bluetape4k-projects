package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Smile 바이너리 포맷을 사용하는 Jackson Serializer 구현체입니다.
 *
 * Smile은 JSON과 1:1 대응하며, 헤더와 종료 마커를 포함하여 스트리밍 처리에 최적화되어 있습니다.
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
