package io.bluetape4k.json

import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String


/**
 * 객체를 JSON으로 직렬화/역직렬화하는 Serializer 의 최상위 인터페이스
 *
 * ```
 * val serializer: JsonSerializer = JacksonSerializer()
 *
 * // object to byte array
 * val bytes = serializer.serialize(data)
 * val data = serializer.deserialize(bytes, Data::class.java)
 * val data2= serializer.deserialize<Data>(bytes)
 *
 *
 * // object to string
 * val jsonText = serializer.serializeAsString(data)
 * val data = serializer.deserializeFromString<Data>(jsonText)
 * ```
 */
interface JsonSerializer {

    /**
     * 객체 상태를 JSON으로 직렬화합니다.
     *
     * @param graph 직렬화할 객체
     * @return JSON 직렬화 결과, 실패 시에는 빈 [ByteArray] 반환
     */
    fun serialize(graph: Any?): ByteArray

    /**
     * JSON으로 직렬화된 [ByteArray]를 읽어, 객체로 변환합니다.
     *
     * @param bytes JSON 직렬화된 데이터
     * @param clazz 역직렬화할 대상 수형
     * @return 역직렬화된 객체, 실패시 null 반환
     */
    fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T?

    /**
     * 객체 상태를 JSON으로 직렬화한 문자열을 반환합니다.
     *
     * @param graph 직렬화할 객체
     * @return JSON 직렬화된 문자열, 싶패 시에는 빈 문자열 반환
     */
    fun serializeAsString(graph: Any?): String =
        graph?.let { serialize(it).toUtf8String() } ?: EMPTY_STRING

    @Deprecated("Use deserializeFromString instead", ReplaceWith("deserializeFromString(jsonText, clazz)"))
    fun <T: Any> deserializeAsString(jsonText: String?, clazz: Class<T>): T? =
        jsonText?.let { deserialize(it.toUtf8Bytes(), clazz) }

    /**
     * JSON 직렬화된 문자열을 읽어, 객체로 변환합니다.
     *
     * @param jsonText JSON 직렬화된 문자열
     * @param clazz 역직렬화할 대상 수형
     * @return 역직렬화된 객체, 실패시 null 반환
     */
    fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? =
        jsonText?.let { deserialize(it.toUtf8Bytes(), clazz) }
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
inline fun <reified T: Any> JsonSerializer.deserialize(bytes: ByteArray?): T? =
    deserialize(bytes, T::class.java)

/**
 * [JsonSerializer]를 이용하여 [jsonText]를 역직렬화하여 객체를 빌드합니다. 실패시 null 반환
 *
 * ```
 * val serializer = JacksonSerializer()
 * val jsonText = serializer.serializeAsString(data)
 * val data = serializer.deserializeAsString<Data>(jsonText)
 * ```
 *
 * @param T 역직렬화할 대상 수형
 * @param jsonText JSON 직렬화된 문자열
 * @return 역직렬화된 객체, 실패 시 null 반환
 */
@Deprecated("Use deserializeFromString instead", ReplaceWith("deserializeFromString<T>(jsonText)"))
inline fun <reified T: Any> JsonSerializer.deserializeAsString(jsonText: String?): T? =
    deserializeAsString(jsonText, T::class.java)

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
inline fun <reified T: Any> JsonSerializer.deserializeFromString(jsonText: String?): T? =
    deserializeFromString(jsonText, T::class.java)
