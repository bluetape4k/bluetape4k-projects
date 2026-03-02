package io.bluetape4k.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray

/**
 * Jackson 라이브러리를 사용하는 [JsonSerializer] 구현체입니다.
 *
 * ## 동작/계약
 * - 기본 [mapper]는 [Jackson.defaultJsonMapper]입니다.
 * - [serialize]는 입력이 null이면 빈 바이트 배열을 반환합니다.
 * - 역직렬화 계열은 입력이 null이면 null을 반환하고, 파싱 실패 시 [JsonSerializationException]을 던집니다.
 * - 입력 객체/바이트 배열은 mutate하지 않고 직렬화 결과를 새로 생성합니다.
 *
 * ```kotlin
 * val serializer = JacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * val value: Map<*, *>? = serializer.deserialize(bytes, Map::class.java)
 * // value?.get("id") == 1
 * ```
 *
 * @param mapper JSON 직렬화/역직렬화에 사용할 ObjectMapper
 */
open class JacksonSerializer(
    val mapper: ObjectMapper = Jackson.defaultJsonMapper,
): JsonSerializer {

    companion object: KLogging()

    /**
     * 객체를 JSON [ByteArray]로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 null이면 빈 바이트 배열을 반환합니다.
     * - 직렬화 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val bytes = JacksonSerializer().serialize(mapOf("name" to "debop"))
     * // bytes.isNotEmpty() == true
     * ```
     * @param graph 직렬화할 객체
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null) {
            return emptyByteArray
        }
        return try {
            requireNotNull(mapper.writeAsBytes(graph)) { "mapper.writeAsBytes returned null." }
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to serialize by Jackson. graphType=${graph.javaClass.name}", e)
        }
    }

    /**
     * JSON [ByteArray]를 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 null이면 null을 반환합니다.
     * - [clazz]로 역직렬화할 수 없으면 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value = JacksonSerializer().deserialize("{\"id\":1}".toByteArray(), Map::class.java)
     * // value?.get("id") == 1
     * ```
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
            throw JsonSerializationException("Fail to deserialize by Jackson. targetType=${clazz.name}", e)
        }
    }

    /**
     * JSON [ByteArray]를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 null이면 null을 반환합니다.
     * - 파싱/타입 변환 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value: Map<String, Int>? = JacksonSerializer().deserialize("{\"id\":1}".toByteArray())
     * // value?.get("id") == 1
     * ```
     * @param bytes JSON 바이트 배열
     */
    inline fun <reified T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson. targetType=${T::class.java.name}", e)
            }
        }

    /**
     * JSON 문자열을 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [jsonText]가 null이면 null을 반환합니다.
     * - 파싱/타입 변환 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val value: Map<String, Int>? = JacksonSerializer().deserializeFromString("{\"id\":1}")
     * // value?.get("id") == 1
     * ```
     * @param jsonText JSON 문자열
     */
    inline fun <reified T: Any> deserializeFromString(jsonText: String?): T? =
        jsonText?.let {
            try {
                mapper.readValue(it, jacksonTypeRef<T>())
            } catch (e: Throwable) {
                throw JsonSerializationException("Fail to deserialize by Jackson. targetType=${T::class.java.name}", e)
            }
        }
}
