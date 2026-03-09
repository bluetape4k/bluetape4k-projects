package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.apache.avro.specific.SpecificRecord

/**
 * Avro `SpecificRecord` 타입 직렬화/역직렬화 API를 제공하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 단건 API는 `SpecificRecord` 1건을, 리스트 API는 동일 스키마 레코드 목록을 처리합니다.
 * - 기본 문자열 API는 바이트 결과를 Base64로 변환하며 역방향도 동일합니다.
 * - `serialize(null)`과 `deserialize(null, ...)`의 null 처리 정책은 메서드 계약을 따릅니다.
 * - 예외/실패 시 반환 정책(`null`, 빈 리스트, 예외)은 구현체에 의해 결정됩니다.
 *
 * ```kotlin
 * val serializer = DefaultAvroSpecificRecordSerializer()
 * val restored = serializer.deserialize<org.apache.avro.specific.SpecificRecord>(null)
 * // restored == null
 * ```
 */
interface AvroSpecificRecordSerializer {

    /**
     * `SpecificRecord` 단건을 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 입력 레코드는 변경하지 않으며, 새 바이트 배열을 생성합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroSpecificRecordSerializer()
     *     .serialize(null as org.apache.avro.specific.SpecificRecord?)
     * // bytes == null
     * ```
     *
     * @param graph 직렬화할 `SpecificRecord`입니다. `null`이면 `null`을 반환합니다.
     */
    fun <T: SpecificRecord> serialize(graph: T?): ByteArray?

    /**
     * Avro 바이트 배열을 지정 `SpecificRecord` 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - [clazz]는 writer schema와 호환 가능한 Avro 타입이어야 합니다.
     * - 실패 시 반환/예외 정책은 구현체를 따릅니다.
     *
     * ```kotlin
     * val restored = DefaultAvroSpecificRecordSerializer()
     *     .deserialize(null, org.apache.avro.specific.SpecificRecord::class.java)
     * // restored == null
     * ```
     *
     * @param avroBytes Avro 바이트 배열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 클래스입니다.
     */
    fun <T: SpecificRecord> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T?

    /**
     * `SpecificRecord`를 Base64 문자열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 내부적으로 [serialize] 결과를 Base64 인코딩합니다.
     *
     * ```kotlin
     * val text = DefaultAvroSpecificRecordSerializer()
     *     .serializeAsString(null as org.apache.avro.specific.SpecificRecord?)
     * // text == null
     * ```
     *
     * @param graph 직렬화할 레코드입니다. `null`이면 `null`을 반환합니다.
     */
    fun <T: SpecificRecord> serializeAsString(graph: T?): String? {
        return graph?.run { serialize(this)?.encodeBase64String() }
    }

    /**
     * Base64 Avro 문자열을 지정 `SpecificRecord` 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroText]가 `null`이면 `null`을 반환합니다.
     * - Base64 디코딩 후 [deserialize]에 위임합니다.
     * - Base64 형식이 잘못되면 `null`을 반환합니다.
     *
     * ```kotlin
     * val restored = DefaultAvroSpecificRecordSerializer()
     *     .deserializeFromString(null, org.apache.avro.specific.SpecificRecord::class.java)
     * // restored == null
     * ```
     *
     * @param avroText Base64 Avro 문자열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 클래스입니다.
     */
    fun <T: SpecificRecord> deserializeFromString(avroText: String?, clazz: Class<T>): T? {
        return avroText?.runCatching { deserialize(this.decodeBase64ByteArray(), clazz) }?.getOrNull()
    }

    /**
     * `SpecificRecord` 리스트를 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [collection]이 `null`이거나 비어 있으면 구현체 정책에 따라 `null` 또는 예외를 반환할 수 있습니다.
     * - 구현체는 보통 첫 요소의 스키마를 기준으로 전체를 기록합니다.
     * - 새 바이트 배열을 할당해 반환합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroSpecificRecordSerializer()
     *     .serializeList(emptyList())
     * // bytes == null
     * ```
     *
     * @param collection 직렬화할 레코드 목록입니다.
     */
    fun <T: SpecificRecord> serializeList(collection: List<T>?): ByteArray?

    /**
     * Avro 바이트 배열을 `SpecificRecord` 리스트로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`/빈 배열일 때의 처리(`emptyList` 등)는 구현체를 따릅니다.
     * - 반환 리스트는 새로 생성됩니다.
     *
     * ```kotlin
     * val list = DefaultAvroSpecificRecordSerializer()
     *     .deserializeList(null, org.apache.avro.specific.SpecificRecord::class.java)
     * // list == emptyList<org.apache.avro.specific.SpecificRecord>()
     * ```
     *
     * @param avroBytes Avro 바이트 배열입니다.
     * @param clazz 리스트 요소 타입 정보입니다.
     */
    fun <T: SpecificRecord> deserializeList(avroBytes: ByteArray?, clazz: Class<T>): List<T>
}

/**
 * Avro 바이트 배열을 reified `SpecificRecord` 타입으로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `T::class.java`를 사용해 [deserialize]를 호출하는 편의 함수입니다.
 * - [avroBytes]가 `null`이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val restored = DefaultAvroSpecificRecordSerializer().deserialize<org.apache.avro.specific.SpecificRecord>(null)
 * // restored == null
 * ```
 *
 * @param avroBytes Avro 바이트 배열입니다.
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserialize(avroBytes: ByteArray?): T? {
    return deserialize(avroBytes, T::class.java)
}

/**
 * Base64 Avro 문자열을 reified `SpecificRecord` 타입으로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `T::class.java`를 사용해 [deserializeFromString]을 호출합니다.
 * - [avroText]가 `null`이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val restored = DefaultAvroSpecificRecordSerializer()
 *     .deserializeFromString<org.apache.avro.specific.SpecificRecord>(null)
 * // restored == null
 * ```
 *
 * @param avroText Base64 Avro 문자열입니다.
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserializeFromString(avroText: String?): T? {
    return deserializeFromString(avroText, T::class.java)
}

/**
 * Avro 바이트 배열을 reified `SpecificRecord` 리스트로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `T::class.java`를 사용해 [deserializeList]를 호출합니다.
 * - null/빈 배열 처리 정책은 구현체 계약을 그대로 따릅니다.
 *
 * ```kotlin
 * val list = DefaultAvroSpecificRecordSerializer()
 *     .deserializeList<org.apache.avro.specific.SpecificRecord>(null)
 * // list == emptyList<org.apache.avro.specific.SpecificRecord>()
 * ```
 *
 * @param avroBytes Avro 바이트 배열입니다.
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserializeList(avroBytes: ByteArray?): List<T> {
    return deserializeList(avroBytes, T::class.java)
}
