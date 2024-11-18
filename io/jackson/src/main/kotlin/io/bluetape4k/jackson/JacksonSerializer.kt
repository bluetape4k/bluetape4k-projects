package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray

/**
 * Jackson JSON 직렬화/역직렬화를 위한 Serializer
 *
 * ```
 * val serializer = JacksonSerializer()
 *
 * // object to byte array
 * val bytes = serializer.serialize(data)
 * val data = serializer.deserialize(bytes, Data::class.java)
 * val data2= serializer.deserialize<Data>(bytes)
 *
 * // object to string
 * val jsonText = serializer.serializeAsString(data)
 * val data = serializer.deserializeFromString<Data>(jsonText)
 * ```
 *
 * @param mapper Jackson [ObjectMapper] 인스턴스
 */
open class JacksonSerializer(
    val mapper: ObjectMapper = Jackson.defaultJsonMapper,
): JsonSerializer {

    companion object: KLogging()

    override fun serialize(graph: Any?): ByteArray {
        return graph?.run { mapper.writeAsBytes(this) } ?: emptyByteArray
    }

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        return bytes?.run { mapper.readValue(this, clazz) }
    }
}
