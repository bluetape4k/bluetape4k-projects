package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String

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
