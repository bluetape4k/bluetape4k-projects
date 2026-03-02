package io.bluetape4k.json

import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String

/**
 * 객체의 JSON 직렬화/역직렬화를 위한 공통 인터페이스입니다.
 *
 * ## 동작/계약
 * - 구현체는 `ByteArray` 기반 `serialize/deserialize`를 핵심 경로로 제공해야 합니다.
 * - 문자열 변환 함수는 UTF-8 인코딩/디코딩으로 바이트 API에 위임합니다.
 * - 직렬화/역직렬화 실패 시 구현체는 [JsonSerializationException] 또는 그 하위 예외를 던질 수 있습니다.
 *
 * ```kotlin
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * val value: Map<*, *>? = serializer.deserialize(bytes)
 * // value?.get("id") == 1
 * ```
 */
interface JsonSerializer {

    /**
     * 객체를 JSON 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 null일 때의 반환 규칙은 구현체 계약을 따릅니다.
     * - 반환 바이트 배열은 UTF-8 JSON 표현을 가정합니다.
     * - 직렬화 실패 시 예외가 발생할 수 있습니다.
     *
     * ```kotlin
     * val bytes = serializer.serialize(mapOf("name" to "debop"))
     * // bytes.isNotEmpty() == true
     * ```
     */
    fun serialize(graph: Any?): ByteArray

    /**
     * JSON 바이트 배열을 지정 타입 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [bytes]가 null이면 null 반환 규칙을 따릅니다.
     * - [clazz] 타입으로 매핑할 수 없으면 예외가 발생할 수 있습니다.
     * - 역직렬화 시 추가 입력 버퍼를 복제할지 여부는 구현체에 따릅니다.
     *
     * ```kotlin
     * val text: String? = serializer.deserialize("hello".toByteArray(), String::class.java)
     * // text == "hello"
     * ```
     */
    fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T?

    /**
     * 객체를 JSON 문자열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 null이면 빈 문자열을 반환합니다.
     * - null이 아니면 [serialize] 결과를 UTF-8 문자열로 변환해 반환합니다.
     * - 예외 규칙은 [serialize] 구현을 따릅니다.
     *
     * ```kotlin
     * val json = serializer.serializeAsString(mapOf("id" to 1))
     * // json.contains("id") == true
     * ```
     */
    fun serializeAsString(graph: Any?): String =
        graph?.let { serialize(it).toUtf8String() }.orEmpty()

    /**
     * JSON 문자열을 지정 타입 객체로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [jsonText]가 null이면 null을 반환합니다.
     * - null이 아니면 UTF-8 바이트 변환 후 [deserialize]로 위임합니다.
     * - 역직렬화 실패 시 예외가 발생할 수 있습니다.
     *
     * ```kotlin
     * val value: Map<*, *>? = serializer.deserializeFromString("{\"id\":1}", Map::class.java)
     * // value?.get("id") == 1.0 || value?.get("id") == 1
     * ```
     */
    fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? =
        jsonText?.let { deserialize(it.toUtf8Bytes(), clazz) }
}

/**
 * reified 타입으로 JSON 바이트 배열을 역직렬화합니다.
 *
 * ## 동작/계약
 * - [deserialize]에 `T::class.java`를 전달해 위임합니다.
 * - [bytes]가 null이면 null 반환 규칙을 따릅니다.
 * - 타입 매핑 실패 시 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val value: String? = serializer.deserialize("hello".toByteArray())
 * // value == "hello"
 * ```
 */
inline fun <reified T: Any> JsonSerializer.deserialize(bytes: ByteArray?): T? =
    deserialize(bytes, T::class.java)

/**
 * reified 타입으로 JSON 문자열을 역직렬화합니다.
 *
 * ## 동작/계약
 * - [deserializeFromString]에 `T::class.java`를 전달해 위임합니다.
 * - [jsonText]가 null이면 null을 반환합니다.
 * - 문자열 파싱/타입 매핑 실패 시 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val value: String? = serializer.deserializeFromString("\"hello\"")
 * // value == "hello"
 * ```
 */
inline fun <reified T: Any> JsonSerializer.deserializeFromString(jsonText: String?): T? =
    deserializeFromString(jsonText, T::class.java)
