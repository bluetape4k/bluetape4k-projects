package io.bluetape4k.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.reference
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty

/**
 * Fastjson2 기반으로 JSON 문자열/JSONB 바이트 직렬화를 제공하는 [JsonSerializer] 구현체입니다.
 *
 * ## 동작/계약
 * - [serialize], [deserialize]는 JSONB 포맷을 사용하고, [serializeAsString], [deserializeFromString]은 JSON 텍스트를 사용합니다.
 * - `serialize(null)`은 빈 바이트 배열을, `serializeAsString(null)`은 빈 문자열을 반환합니다.
 * - 역직렬화 입력이 `null`이면 `null`을 반환합니다.
 * - fastjson2 처리 실패는 [JsonSerializationException]으로 감싸서 던집니다.
 *
 * ```kotlin
 * val serializer = FastjsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * val restored = serializer.deserialize<Map<String, Int>>(bytes)
 * // restored == mapOf("id" to 1)
 * ```
 */
class FastjsonSerializer: JsonSerializer {

    companion object: KLogging() {
        /**
         * 지연 초기화되는 기본 [FastjsonSerializer] 인스턴스입니다.
         *
         * ## 동작/계약
         * - 첫 접근 시 1회 생성되고 이후 동일 인스턴스를 재사용합니다.
         * - 직렬화 상태를 내부에 보관하지 않아 여러 호출에서 공유해도 동작이 같습니다.
         *
         * ```kotlin
         * val same = FastjsonSerializer.Default === FastjsonSerializer.Default
         * // same == true
         * ```
         */
        val Default by lazy { FastjsonSerializer() }
    }

    /**
     * 객체를 JSONB 형식의 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 빈 바이트 배열(`emptyByteArray`)을 반환합니다.
     * - `JSONB.toBytes(graph)` 결과를 새 바이트 배열로 반환합니다.
     * - 직렬화 중 예외가 발생하면 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val bytes = FastjsonSerializer().serialize(mapOf("name" to "blue"))
     * // bytes.isNotEmpty() == true
     * ```
     *
     * @param graph 직렬화할 객체입니다. `null`이면 빈 바이트 배열을 반환합니다.
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
     * JSONB 바이트 배열을 지정 클래스 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 `null`이면 `null`을 반환합니다.
     * - `JSONB.parseObject(bytes, clazz)`를 호출해 객체를 생성합니다.
     * - 역직렬화 실패는 [JsonSerializationException]으로 변환됩니다.
     *
     * ```kotlin
     * val serializer = FastjsonSerializer()
     * val bytes = serializer.serialize(listOf(1, 2, 3))
     * val restored = serializer.deserialize(bytes, List::class.java)
     * // restored == listOf(1, 2, 3)
     * ```
     *
     * @param bytes JSONB 바이트 배열입니다. `null`이거나 빈 배열이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 클래스입니다.
     */
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        if(bytes.isNullOrEmpty()) {
            return null
        }
        return try {
            JSONB.parseObject(bytes, clazz)
        } catch (e: Throwable) {
            throw JsonSerializationException("Fail to deserialize by Fastjson2. targetType=${clazz.name}", e)
        }
    }

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 빈 문자열을 반환합니다.
     * - `toJSONString()` 결과 문자열을 새로 생성해 반환합니다.
     * - 직렬화 실패는 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val json = FastjsonSerializer().serializeAsString(mapOf("id" to 7))
     * // json == "{\"id\":7}"
     * ```
     *
     * @param graph 직렬화할 객체입니다. `null`이면 빈 문자열을 반환합니다.
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
     * JSON 문자열을 지정 클래스 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [jsonText]가 `null`이면 `null`을 반환합니다.
     * - `JSON.parseObject(jsonText, clazz)`를 사용합니다.
     * - 파싱 실패는 [JsonSerializationException]으로 감싸서 던집니다.
     *
     * ```kotlin
     * val serializer = FastjsonSerializer()
     * val user = serializer.deserializeFromString("""{"id":1}""", Map::class.java)
     * // user == mapOf("id" to 1)
     * ```
     *
     * @param jsonText 역직렬화할 JSON 문자열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 클래스입니다.
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

    /**
     * JSONB 바이트 배열을 reified 타입 [T]로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 `null`이면 `null`을 반환합니다.
     * - 타입 파라미터가 있으면 `reference<T>()`, 없으면 `T::class.java` 경로를 사용합니다.
     * - 역직렬화 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val serializer = FastjsonSerializer()
     * val parsed = serializer.deserialize<List<Int>>(serializer.serialize(listOf(1, 2, 3)))
     * // parsed == listOf(1, 2, 3)
     * ```
     *
     * @param bytes JSONB 바이트 배열입니다. `null`이거나 빈 배열이면 `null`을 반환합니다.
     */
    inline fun <reified T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.takeIf { it.isNotEmpty() }?.let {
            try {
                val clazz = T::class.java
                if (clazz.typeParameters.isEmpty()) {
                    JSONB.parseObject(it, clazz)
                } else {
                    JSONB.parseObject(it, reference<T>())
                }
            } catch (e: Throwable) {
                throw JsonSerializationException(
                    "Fail to deserialize by Fastjson2. targetType=${T::class.java.name}",
                    e
                )
            }
        }

    /**
     * JSON 문자열을 reified 타입 [T]로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [jsonText]가 `null`이면 `null`을 반환합니다.
     * - 타입 파라미터가 있으면 `reference<T>()`, 없으면 `T::class.java` 경로를 사용합니다.
     * - 파싱 실패 시 [JsonSerializationException]을 던집니다.
     *
     * ```kotlin
     * val serializer = FastjsonSerializer()
     * val parsed = serializer.deserializeFromString<Map<String, Int>>("""{"id":3}""")
     * // parsed == mapOf("id" to 3)
     * ```
     *
     * @param jsonText 역직렬화할 JSON 문자열입니다. `null`이면 `null`을 반환합니다.
     */
    inline fun <reified T: Any> deserializeFromString(jsonText: String?): T? =
        jsonText?.let {
            try {
                val clazz = T::class.java
                if (clazz.typeParameters.isEmpty()) {
                    JSON.parseObject(it, clazz)
                } else {
                    JSON.parseObject(it, reference<T>())
                }
            } catch (e: Throwable) {
                throw JsonSerializationException(
                    "Fail to deserialize string by Fastjson2. targetType=${T::class.java.name}",
                    e
                )
            }
        }
}
