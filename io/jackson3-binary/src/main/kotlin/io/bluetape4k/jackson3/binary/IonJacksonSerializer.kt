package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.ion.IonObjectMapper

/**
 * Amazon Ion 바이너리 포맷을 사용하는 Jackson Serializer 구현체입니다.
 *
 * Ion은 리치 타입 시스템과 네이티브 타입 ID를 지원하며, 바이너리와 텍스트 형식을 모두 제공합니다.
 *
 * ```
 * val serializer = IonJacksonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [IonObjectMapper] 인스턴스
 */
class IonJacksonSerializer(
    mapper: IonObjectMapper = JacksonBinary.ION.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
