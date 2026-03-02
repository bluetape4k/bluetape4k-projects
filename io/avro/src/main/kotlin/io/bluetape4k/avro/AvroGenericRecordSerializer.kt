package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord

/**
 * Avro `GenericRecord`를 스키마 기반으로 직렬화/역직렬화하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 모든 API는 [schema]를 필수 입력으로 사용하며 코드 생성 없이 동적 레코드를 처리합니다.
 * - `serialize(schema, null)`과 `deserialize(schema, null)`은 `null`을 반환합니다.
 * - 문자열 API는 바이트 API의 Base64 인코딩/디코딩 래퍼입니다.
 * - 스키마/데이터 불일치 시 실패 처리(`null`/예외)는 구현체 정책을 따릅니다.
 *
 * ```kotlin
 * val serializer = DefaultAvroGenericRecordSerializer()
 * val restored = serializer.deserialize(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL), null)
 * // restored == null
 * ```
 */
interface AvroGenericRecordSerializer {

    /**
     * `GenericRecord`를 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - [schema]와 레코드 구조가 호환되어야 합니다.
     * - 입력 레코드는 변경하지 않고 새 바이트 배열을 반환합니다.
     *
     * ```kotlin
     * val schema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
     * val bytes = DefaultAvroGenericRecordSerializer().serialize(schema, null)
     * // bytes == null
     * ```
     *
     * @param schema 직렬화에 사용할 Avro 스키마입니다.
     * @param graph 직렬화할 레코드입니다. `null`이면 `null`을 반환합니다.
     */
    fun serialize(schema: Schema, graph: GenericRecord?): ByteArray?

    /**
     * Avro 바이트 배열을 [GenericData.Record]로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - reader schema는 [schema]를 사용합니다.
     * - 역직렬화 실패 시 반환/예외 정책은 구현체를 따릅니다.
     *
     * ```kotlin
     * val schema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
     * val record = DefaultAvroGenericRecordSerializer().deserialize(schema, null)
     * // record == null
     * ```
     *
     * @param schema 역직렬화에 사용할 Avro 스키마입니다.
     * @param avroBytes Avro 바이트 배열입니다. `null`이면 `null`을 반환합니다.
     */
    fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record?

    /**
     * `GenericRecord`를 Base64 Avro 문자열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 내부적으로 [serialize] 후 Base64 인코딩합니다.
     *
     * ```kotlin
     * val schema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
     * val text = DefaultAvroGenericRecordSerializer().serializeAsString(schema, null)
     * // text == null
     * ```
     *
     * @param schema 직렬화에 사용할 Avro 스키마입니다.
     * @param graph 직렬화할 레코드입니다.
     */
    fun serializeAsString(schema: Schema, graph: GenericRecord?): String? {
        return graph?.run { serialize(schema, this)?.encodeBase64String() }
    }

    /**
     * Base64 Avro 문자열을 [GenericData.Record]로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroText]가 `null`이면 `null`을 반환합니다.
     * - Base64 디코딩 후 [deserialize]에 위임합니다.
     *
     * ```kotlin
     * val schema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
     * val record = DefaultAvroGenericRecordSerializer().deserializeFromString(schema, null)
     * // record == null
     * ```
     *
     * @param schema 역직렬화에 사용할 Avro 스키마입니다.
     * @param avroText Base64 Avro 문자열입니다.
     */
    fun deserializeFromString(schema: Schema, avroText: String?): GenericData.Record? {
        return avroText?.run { deserialize(schema, this.decodeBase64ByteArray()) }
    }
}
