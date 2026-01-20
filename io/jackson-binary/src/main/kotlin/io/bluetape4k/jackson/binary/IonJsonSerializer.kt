package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.ion.IonObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Binary JSON 직렬화를 위한 Ion Serializer
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
