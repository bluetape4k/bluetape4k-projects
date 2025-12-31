package io.bluetape4k.jackson3

import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import tools.jackson.databind.ObjectMapper

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


    /**
     * [JsonSerializer]를 이용하여 [bytes]를 역직렬화하여 객체를 빌드합니다. 실패시 null 반환
     *
     * ```
     * val serializer = JacksonSerializer()
     * val bytes = serializer.serialize(data)
     * val data = serializer.deserialize<Data>(bytes)
     * ```
     *
     * @param T 역직렬화할 대상 수형
     * @param bytes JSON 직렬화된 ByteArray
     * @return 역직렬화된 객체, 실패 시 null 반환
     */
    inline fun <reified T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.run { mapper.readValueOrNull<T>(bytes) }

    /**
     * [JsonSerializer]를 이용하여 [jsonText]를 역직렬화하여 객체를 빌드합니다. 실패시 null 반환
     *
     * ```
     * val serializer = JacksonSerializer()
     * val jsonText = serializer.serializeAsString(data)
     * val data = serializer.deserializeFromString<Data>(jsonText)
     * ```
     *
     * @param T 역직렬화할 대상 수형
     * @param jsonText JSON 직렬화된 문자열
     * @return 역직렬화된 객체, 실패 시 null 반환
     */
    inline fun <reified T: Any> deserializeFromString(jsonText: String?): T? =
        jsonText?.run { mapper.readValueOrNull<T>(jsonText) }
}
