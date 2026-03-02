package io.bluetape4k.jackson3

import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import tools.jackson.databind.ObjectMapper

/**
 * Jackson 3 기반 [JsonSerializer] 구현체입니다.
 *
 * ## 동작/계약
 * - [serialize]는 입력이 null이면 빈 바이트 배열을 반환합니다.
 * - 역직렬화 계열은 입력이 null이면 null을 반환하고 실패 시 [JsonSerializationException]을 던집니다.
 * - 기본 매퍼는 [Jackson.defaultJsonMapper]입니다.
 *
 * ```kotlin
 * val serializer = JacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * val value: Map<*, *>? = serializer.deserialize(bytes, Map::class.java)
 * // value?.get("id") == 1
 * ```
 *
 * @param mapper JSON 처리에 사용할 ObjectMapper
 */
open class JacksonSerializer(
    val mapper: ObjectMapper = Jackson.defaultJsonMapper,
): JsonSerializer {

    companion object: KLogging()

    /**
     * 객체를 JSON [ByteArray]로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 null이면 빈 배열을 반환합니다.
     * - 직렬화 실패 시 [JsonSerializationException]을 던집니다.
     * @param graph 직렬화할 객체
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
     * ## 동작/계약
     * - [bytes]가 null이면 null을 반환합니다.
     * - 파싱/타입 매핑 실패 시 [JsonSerializationException]을 던집니다.
     * @param bytes JSON 바이트 배열
     * @param clazz 대상 타입 클래스
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
