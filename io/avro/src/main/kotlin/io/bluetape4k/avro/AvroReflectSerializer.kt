package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import java.nio.ByteBuffer

/**
 * Reflection 기반으로 Avro 바이트/문자열 직렬화 API를 제공하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 구현체는 클래스 메타데이터를 Reflection으로 읽어 Avro 스키마를 구성해 직렬화/역직렬화합니다.
 * - `serialize(null)`은 `null`, `deserialize(null, ...)`은 `null`을 반환합니다.
 * - 기본 문자열 API는 Base64 변환만 담당하고 실제 직렬화/역직렬화는 바이트 API에 위임합니다.
 * - Reflection 경로는 `SpecificRecord` 경로보다 런타임 오버헤드가 커질 수 있습니다.
 *
 * ```kotlin
 * val serializer = DefaultAvroReflectSerializer()
 * val bytes = serializer.serialize(TestMessageProvider.createEmployee())
 * val restored = serializer.deserialize<io.bluetape4k.avro.message.examples.Employee>(bytes)
 * // restored != null
 * ```
 *
 * The [issue #1039 evidence](https://github.com/bluetape4k/bluetape4k-projects/blob/develop/docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)
 * measured reflect paths only; both optimized comparisons were inconclusive and defaults remain compatibility fallbacks.
 */
interface AvroReflectSerializer {

    /**
     * 객체를 Avro Reflection 경로로 바이트 배열에 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 수신 객체는 변경하지 않으며, 직렬화된 새 바이트 배열을 반환합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroReflectSerializer().serialize(TestMessageProvider.createEmployee())
     * // bytes != null
     * ```
     *
     * @param graph 직렬화할 객체입니다. `null`이면 `null`을 반환합니다.
     */
    fun <T> serialize(graph: T?): ByteArray?

    /**
     * [graph]를 호출자 소유 [target]의 현재 위치부터 직렬화합니다.
     *
     * This default is an allocating fallback that calls [serialize] first. A read-only target fails with
     * `ReadOnlyBufferException` before serializer code runs. A null [serialize] result writes nothing and returns `0`;
     * insufficient remaining space fails with the raw `BufferOverflowException`.
     *
     * Success advances only the position by the returned count. Limit, capacity, and byte order remain unchanged.
     * Failure restores the original position and rethrows the same throwable, including any `Error`; failed content is
     * unspecified and is not rolled back. Normal JDK mark rules apply on success. The caller owns [target] and must keep
     * it thread-confined while this call is active.
     */
    fun <T> serializeTo(graph: T?, target: ByteBuffer): Int =
        serializeNullableTo(target) { serialize(graph) }

    /**
     * Avro 바이트 배열을 Reflection 경로로 지정 타입에 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - 구현체는 [clazz]를 기준으로 Avro reader를 구성합니다.
     * - 역직렬화 실패 시 반환값/예외 정책은 구현체를 따릅니다.
     *
     * ```kotlin
     * val serializer = DefaultAvroReflectSerializer()
     * val employee = serializer.deserialize(bytes = null, clazz = io.bluetape4k.avro.message.examples.Employee::class.java)
     * // employee == null
     * ```
     *
     * @param avroBytes Avro 바이트 배열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 타입 정보입니다.
     */
    fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T?

    /**
     * `[source.position(), source.limit())` 범위의 신뢰된 호출자 한정 바이트를 [clazz]로 역직렬화합니다.
     *
     * This allocating fallback copies the remaining bytes to a new [ByteArray] before calling [deserialize]. Heap,
     * direct, sliced, and read-only sources are supported. Position, limit, mark, and byte order are preserved on every
     * path. The caller owns [source], must not mutate or share it concurrently, and must bound untrusted input first.
     */
    fun <T> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        deserialize(copyRemaining(source), clazz)

    /**
     * 객체를 Base64 문자열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 내부적으로 [serialize] 결과를 Base64 인코딩합니다.
     * - 입력 객체를 변경하지 않습니다.
     *
     * ```kotlin
     * val text = DefaultAvroReflectSerializer().serializeAsString(TestMessageProvider.createEmployee())
     * // text != null
     * ```
     *
     * @param graph 직렬화할 객체입니다. `null`이면 `null`을 반환합니다.
     */
    fun <T> serializeAsString(graph: T?): String? {
        return graph?.run { serialize(this)?.encodeBase64String() }
    }

    /**
     * Base64 Avro 문자열을 지정 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroText]가 `null`이면 `null`을 반환합니다.
     * - Base64 디코딩 후 [deserialize]에 위임합니다.
     * - Base64 형식이 잘못되면 `null`을 반환합니다.
     *
     * ```kotlin
     * val serializer = DefaultAvroReflectSerializer()
     * val restored = serializer.deserializeFromString<io.bluetape4k.avro.message.examples.Employee>(null)
     * // restored == null
     * ```
     *
     * @param avroText Base64 문자열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 타입 정보입니다.
     */
    fun <T> deserializeFromString(avroText: String?, clazz: Class<T>): T? {
        return avroText?.runCatching { deserialize(this.decodeBase64ByteArray(), clazz) }?.getOrNull()
    }
}

/**
 * Avro 바이트 배열을 reified 타입 [T]로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `T::class.java`를 사용해 [deserialize]를 호출하는 편의 함수입니다.
 * - [avroBytes]가 `null`이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val restored = DefaultAvroReflectSerializer()
 *     .deserialize<io.bluetape4k.avro.message.examples.Employee>(null)
 * // restored == null
 * ```
 *
 * @param avroBytes Avro 바이트 배열입니다.
 */
inline fun <reified T: Any> AvroReflectSerializer.deserialize(avroBytes: ByteArray?): T? {
    return deserialize(avroBytes, T::class.java)
}

/**
 * [source]에 남은 바이트를 [AvroReflectSerializer.deserializeFrom]을 통해 [clazz]로 역직렬화합니다.
 */
fun <T> AvroReflectSerializer.deserialize(source: ByteBuffer, clazz: Class<T>): T? =
    deserializeFrom(source, clazz)

/**
 * [source]에 남은 신뢰된 호출자 한정 바이트를 reified [T]로 역직렬화합니다.
 *
 * 할당 기반 fallback, 호출자 소유권, 스레드 한정, source 상태 보존 규칙은
 * [AvroReflectSerializer.deserializeFrom] apply.
 */
inline fun <reified T: Any> AvroReflectSerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source, T::class.java)

/**
 * Base64 Avro 문자열을 reified 타입 [T]로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `T::class.java`를 사용해 [deserializeFromString]을 호출합니다.
 * - [avroText]가 `null`이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val restored = DefaultAvroReflectSerializer()
 *     .deserializeFromString<io.bluetape4k.avro.message.examples.Employee>(null)
 * // restored == null
 * ```
 *
 * @param avroText Base64 Avro 문자열입니다.
 */
inline fun <reified T: Any> AvroReflectSerializer.deserializeFromString(avroText: String?): T? {
    return deserializeFromString(avroText, T::class.java)
}
