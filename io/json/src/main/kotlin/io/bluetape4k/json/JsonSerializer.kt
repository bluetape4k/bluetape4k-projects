package io.bluetape4k.json

import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

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
 *
 * interface default ByteBuffer 메서드는 할당 기반 호환성 fallback입니다. 구체 backend 판정은
 * the [issue #1039 allocation evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md).
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
     * [graph]를 직렬화하고 결과 바이트를 호출자 소유 [target]에 씁니다.
     *
     * This interface default is an allocating fallback: it first obtains a [ByteArray] from [serialize], then writes
     * that array to [target]. Null input and zero-byte results retain [serialize]'s existing policy. On success, the
     * returned count is exactly the number of bytes written and is bounded by [Int.MAX_VALUE].
     *
     * target은 동기적으로 빌려 쓰며 호출 뒤 보관하지 않습니다. 이 메서드는 flush 또는 close를 수행하지 않습니다
     * it. Serializer and destination failures propagate unchanged; a failed destination may already contain partial
     * output.
     *
     * @return the exact number of bytes written
     */
    @Throws(IOException::class)
    fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
        val bytes = serialize(graph)
        target.write(bytes)
        return bytes.size
    }

    /**
     * [graph]를 호출자 소유 [target]의 현재 위치부터 직렬화합니다.
     *
     * This default is an allocating fallback: it obtains a [ByteArray] from [serialize] before copying it into [target].
     * 읽기 전용 target은 serializer 코드 실행 전 `ReadOnlyBufferException`으로 실패합니다. null 입력은
     * existing [serialize] policy, and a zero-byte result returns `0`. Insufficient remaining space fails with the raw
     * `BufferOverflowException`.
     *
     * Success advances only the position by the returned count; limit, capacity, and byte order remain unchanged.
     * Failure restores the original position and rethrows the same throwable, including any `Error`. Failed content is
     * unspecified and is not rolled back. Normal JDK mark invalidation rules apply on success. The caller owns the
     * buffer and must keep it thread-confined while this call is active.
     *
     * @return the number of bytes written
     */
    fun serializeTo(graph: Any?, target: ByteBuffer): Int =
        serializeTo(target) { serialize(graph) }

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
     * `[source.position(), source.limit())` 범위의 신뢰된 호출자 한정 바이트를 [clazz]로 역직렬화합니다.
     *
     * This default is an allocating fallback that copies the remaining bytes to a new [ByteArray] before delegating to
     * [deserialize]. It supports heap, direct, sliced, and read-only sources while preserving the source position,
     * limit, mark, and byte order on every path. The caller owns the source and must not mutate or share it concurrently
     * during the call. Bound untrusted input before invoking this method.
     */
    fun <T: Any> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        deserialize(copyRemaining(source), clazz)

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
 * [source]에 남은 바이트를 [JsonSerializer.deserializeFrom]을 통해 [clazz]로 역직렬화합니다.
 *
 * 할당 기반 fallback, 호출자 소유권, 신뢰된 bounded-input, 스레드 한정, source 상태 보존
 * rules of [JsonSerializer.deserializeFrom] apply.
 */
fun <T: Any> JsonSerializer.deserialize(source: ByteBuffer, clazz: Class<T>): T? =
    deserializeFrom(source, clazz)

/**
 * [source]에 남은 바이트를 [JsonSerializer.deserializeFrom]을 통해 reified [T]로 역직렬화합니다.
 *
 * 할당 기반 fallback, 호출자 소유권, 신뢰된 bounded-input, 스레드 한정, source 상태 보존
 * rules of [JsonSerializer.deserializeFrom] apply.
 */
inline fun <reified T: Any> JsonSerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source, T::class.java)

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
