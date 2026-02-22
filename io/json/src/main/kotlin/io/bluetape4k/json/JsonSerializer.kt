package io.bluetape4k.json

import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String

/**
 * 객체를 JSON으로 직렬화/역직렬화하는 Serializer의 최상위 인터페이스입니다.
 *
 * 이 인터페이스를 구현하여 다양한 JSON 라이브러리(Jackson, Fastjson2 등)를
 * 동일한 API로 사용할 수 있습니다.
 *
 * ### 사용 예시
 *
 * ```kotlin
 * val serializer: JsonSerializer = JacksonSerializer()
 *
 * // 바이트 배열 직렬화/역직렬화
 * val bytes = serializer.serialize(data)
 * val restored = serializer.deserialize<Data>(bytes)
 *
 * // 문자열 직렬화/역직렬화
 * val jsonText = serializer.serializeAsString(data)
 * val restored2 = serializer.deserializeFromString<Data>(jsonText)
 * ```
 *
 * 구현체 예시: `JacksonSerializer` (bluetape4k-jackson), `FastjsonSerializer` (bluetape4k-fastjson2)
 */
interface JsonSerializer {

    /**
     * 객체를 JSON [ByteArray]로 직렬화합니다.
     *
     * @param graph 직렬화할 객체. null인 경우 빈 [ByteArray] 반환
     * @return JSON 직렬화 결과
     * @throws JsonSerializationException 직렬화 실패 시
     */
    fun serialize(graph: Any?): ByteArray

    /**
     * JSON [ByteArray]를 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param bytes JSON 직렬화된 바이트 배열. null이면 null 반환
     * @param clazz 역직렬화할 대상 클래스
     * @return 역직렬화된 객체
     * @throws JsonSerializationException 역직렬화 실패 시
     */
    fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T?

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     *
     * 내부적으로 [serialize]를 호출한 뒤 UTF-8 문자열로 변환합니다.
     *
     * @param graph 직렬화할 객체. null인 경우 빈 문자열 반환
     * @return JSON 문자열
     * @throws JsonSerializationException 직렬화 실패 시
     */
    fun serializeAsString(graph: Any?): String =
        graph?.let { serialize(it).toUtf8String() }.orEmpty()

    /**
     * JSON 문자열을 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * 내부적으로 UTF-8 바이트 배열로 변환한 뒤 [deserialize]를 호출합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param jsonText JSON 직렬화된 문자열. null이면 null 반환
     * @param clazz 역직렬화할 대상 클래스
     * @return 역직렬화된 객체
     * @throws JsonSerializationException 역직렬화 실패 시
     */
    fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? =
        jsonText?.let { deserialize(it.toUtf8Bytes(), clazz) }
}

/**
 * [JsonSerializer]를 이용하여 [bytes]를 역직렬화하여 객체를 빌드합니다.
 *
 * ```
 * val serializer = JacksonSerializer()
 * val bytes = serializer.serialize(data)
 * val data = serializer.deserialize<Data>(bytes)
 * ```
 *
 * @param T 역직렬화할 대상 수형
 * @param bytes JSON 직렬화된 ByteArray
 * @return 역직렬화된 객체
 * @throws JsonSerializationException 역직렬화 실패 시
 */
inline fun <reified T: Any> JsonSerializer.deserialize(bytes: ByteArray?): T? =
    deserialize(bytes, T::class.java)

/**
 * [JsonSerializer]를 이용하여 [jsonText]를 역직렬화하여 객체를 빌드합니다.
 *
 * ```
 * val serializer = JacksonSerializer()
 * val jsonText = serializer.serializeAsString(data)
 * val data = serializer.deserializeFromString<Data>(jsonText)
 * ```
 *
 * @param T 역직렬화할 대상 수형
 * @param jsonText JSON 직렬화된 문자열
 * @return 역직렬화된 객체
 * @throws JsonSerializationException 역직렬화 실패 시
 */
inline fun <reified T: Any> JsonSerializer.deserializeFromString(jsonText: String?): T? =
    deserializeFromString(jsonText, T::class.java)
