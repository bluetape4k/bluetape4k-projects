package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.ion.IonObjectMapper

/**
 * Binary JSON 직렬화를 위한 Ion Serializer
 *
 * 이 클래스는 더 이상 사용되지 않습니다. [IonJacksonSerializer]를 대신 사용하세요.
 *
 * ```
 * val serializer = IonJsonSerializer()
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 *
 * @param mapper Jackson [IonObjectMapper] 인스턴스
 */
@Deprecated("use IonJacksonSerializer", replaceWith = ReplaceWith("IonJacksonSerializer"))
class IonJsonSerializer(
    mapper: IonObjectMapper = JacksonBinary.ION.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()

}
