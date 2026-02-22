package io.bluetape4k.fastjson2

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray

/**
 * [Fastjson2](https://github.com/alibaba/fastjson2) 라이브러리를 사용하는 [JsonSerializer] 구현체입니다.
 *
 * 바이트 배열 직렬화에는 JSONB(바이너리 JSON) 형식을 사용하여 성능이 우수하며,
 * 문자열 직렬화에는 표준 JSON 형식을 사용합니다.
 *
 * ### 사용 예시
 *
 * ```kotlin
 * val serializer = FastjsonSerializer()
 *
 * // JSONB 바이너리 직렬화/역직렬화
 * val bytes = serializer.serialize(data)
 * val restored = serializer.deserialize<Data>(bytes)
 *
 * // JSON 문자열 직렬화/역직렬화
 * val jsonText = serializer.serializeAsString(data)
 * val restored2 = serializer.deserializeFromString<Data>(jsonText)
 * ```
 *
 * @see JsonSerializer
 * @see com.alibaba.fastjson2.JSONB
 */
class FastjsonSerializer: JsonSerializer {

    companion object: KLogging() {
        /** 기본 [FastjsonSerializer] 인스턴스 (지연 초기화) */
        val Default by lazy { FastjsonSerializer() }
    }

    /**
     * 객체를 JSONB 바이너리 형식의 [ByteArray]로 직렬화합니다.
     *
     * JSONB는 Fastjson2의 바이너리 JSON 형식으로, 표준 JSON보다 빠른 직렬화/역직렬화 성능을 제공합니다.
     *
     * @param graph 직렬화할 객체. null인 경우 빈 [ByteArray] 반환
     * @return JSONB 직렬화된 바이트 배열
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null) {
            return emptyByteArray
        }
        return try {
            JSONB.toBytes(graph)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to serialize by Fastjson2. graphType=${graph.javaClass.name}", e)
        }
    }

    /**
     * JSONB 바이너리 형식의 [ByteArray]를 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param bytes JSONB 직렬화된 바이트 배열. null이면 null 반환
     * @param clazz 역직렬화할 대상 클래스
     * @return 역직렬화된 객체. 실패 시 null 반환
     */
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        if (bytes == null) {
            return null
        }
        return try {
            JSONB.parseObject(bytes, clazz)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize by Fastjson2. targetType=${clazz.name}", e)
        }
    }

    /**
     * 객체를 표준 JSON 문자열로 직렬화합니다.
     *
     * [serialize]와 달리 표준 JSON 텍스트 형식을 사용하므로 사람이 읽을 수 있습니다.
     *
     * @param graph 직렬화할 객체. null인 경우 빈 문자열 반환
     * @return JSON 문자열
     */
    override fun serializeAsString(graph: Any?): String {
        if (graph == null) {
            return ""
        }
        return try {
            graph.toJSONString()
        } catch (e: Throwable) {
            throw JsonSerializationException(
                "Fail to serialize string by Fastjson2. graphType=${graph.javaClass.name}",
                e
            )
        }
    }

    /**
     * JSON 문자열을 읽어 지정된 타입의 객체로 역직렬화합니다.
     *
     * @param T 역직렬화 대상 타입
     * @param jsonText JSON 문자열. null이면 null 반환
     * @param clazz 역직렬화할 대상 클래스
     * @return 역직렬화된 객체. 실패 시 null 반환
     */
    override fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? {
        if (jsonText == null) {
            return null
        }
        return try {
            com.alibaba.fastjson2.JSON.parseObject(jsonText, clazz)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize string by Fastjson2. targetType=${clazz.name}", e)
        }
    }
}

/**
 * JSONB 바이너리 형식의 [ByteArray]를 읽어 지정된 타입 [T]로 역직렬화합니다.
 *
 * reified 타입 파라미터를 사용하여 클래스 명시 없이 호출할 수 있습니다.
 *
 * ```kotlin
 * val user = serializer.deserialize<User>(bytes)
 * ```
 *
 * @param T 역직렬화 대상 타입
 * @param bytes JSONB 직렬화된 바이트 배열
 * @return 역직렬화된 객체. null이거나 실패 시 null 반환
 */
inline fun <reified T: Any> FastjsonSerializer.deserialize(bytes: ByteArray?): T? =
    deserialize(bytes, T::class.java)

/**
 * JSON 문자열을 읽어 지정된 타입 [T]로 역직렬화합니다.
 *
 * reified 타입 파라미터를 사용하여 클래스 명시 없이 호출할 수 있습니다.
 *
 * ```kotlin
 * val user = serializer.deserialize<User>(jsonText)
 * ```
 *
 * @param T 역직렬화 대상 타입
 * @param jsonText JSON 문자열
 * @return 역직렬화된 객체. null이거나 실패 시 null 반환
 */
inline fun <reified T: Any> FastjsonSerializer.deserialize(jsonText: String?): T? =
    deserializeFromString(jsonText, T::class.java)
