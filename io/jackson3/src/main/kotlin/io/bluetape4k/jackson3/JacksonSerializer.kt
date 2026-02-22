package io.bluetape4k.jackson3

import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import tools.jackson.databind.ObjectMapper

/**
 * Jackson 3.x 라이브러리를 사용하는 [JsonSerializer] 구현체입니다.
 *
 * [ObjectMapper]를 통해 JSON 직렬화/역직렬화를 수행하며,
 * 기본적으로 [Jackson.defaultJsonMapper]를 사용합니다.
 *
 * ### 사용 예시
 *
 * ```kotlin
 * val serializer = JacksonSerializer()
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
 * @param mapper JSON 처리에 사용할 [ObjectMapper]. 기본값은 [Jackson.defaultJsonMapper]
 * @see JsonSerializer
 * @see Jackson.defaultJsonMapper
 */
open class JacksonSerializer(
    val mapper: ObjectMapper = Jackson.defaultJsonMapper,
): JsonSerializer {

    companion object: KLogging()

    /**
     * 객체를 JSON [ByteArray]로 직렬화합니다.
     *
     * @param graph 직렬화할 객체. null인 경우 빈 [ByteArray] 반환
     * @return JSON 바이트 배열
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null) {
            return emptyByteArray
        }
        return try {
            requireNotNull(mapper.writeAsBytes(graph)) { "mapper.writeAsBytes returned null." }
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to serialize by Jackson3. graphType=${graph.javaClass.name}", e)
        }
    }

    /**
     * JSON [ByteArray]를 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param bytes JSON 바이트 배열. null이면 null 반환
     * @param clazz 역직렬화할 대상 클래스
     * @return 역직렬화된 객체. 실패 시 null 반환
     */
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        if (bytes == null) {
            return null
        }
        return try {
            mapper.readValue(bytes, clazz)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${clazz.name}", e)
        }
    }

    /**
     * JSON [ByteArray]를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param bytes JSON 바이트 배열
     * @return 역직렬화된 객체. null이거나 실패 시 null 반환
     */
    inline fun <reified T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${T::class.java.name}", e)
            }
        }

    /**
     * JSON 문자열을 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param jsonText JSON 문자열
     * @return 역직렬화된 객체. null이거나 실패 시 null 반환
     */
    inline fun <reified T: Any> deserializeFromString(jsonText: String?): T? =
        jsonText?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson3. targetType=${T::class.java.name}", e)
            }
        }
}
