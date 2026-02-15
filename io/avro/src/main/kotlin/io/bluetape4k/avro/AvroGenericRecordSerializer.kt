package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord

/**
 * Avro [GenericRecord]를 [ByteArray]로 직렬화하고, [GenericRecord]로 역직렬화를 수행하는 인터페이스입니다.
 *
 * [GenericRecord]는 스키마 정보만으로 동작하므로, 코드 생성 없이도 유연하게 Avro 데이터를 다룰 수 있습니다.
 * 스키마가 런타임에 결정되는 경우나, 동적으로 Avro 데이터를 처리해야 하는 경우에 적합합니다.
 *
 * ```
 * val serializer = DefaultAvroGenericRecordSerializer()
 * val schema = Employee.getClassSchema()
 * val record: GenericRecord = ...
 *
 * // ByteArray로 직렬화/역직렬화
 * val bytes = serializer.serialize(schema, record)
 * val deserialized = serializer.deserialize(schema, bytes)
 *
 * // Base64 문자열로 직렬화/역직렬화
 * val text = serializer.serializeAsString(schema, record)
 * val fromText = serializer.deserializeFromString(schema, text)
 * ```
 *
 * @see AvroSpecificRecordSerializer
 * @see AvroReflectSerializer
 */
interface AvroGenericRecordSerializer {

    /**
     * [GenericRecord]를 Avro 바이너리 형식으로 직렬화합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param graph 직렬화할 Avro [GenericRecord] 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이면 null 반환
     */
    fun serialize(schema: Schema, graph: GenericRecord?): ByteArray?

    /**
     * Avro 바이너리 데이터를 [GenericData.Record]로 역직렬화합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param avroBytes 직렬화된 데이터
     * @return 역직렬화된 Avro [Record], [avroBytes]가 null이거나 실패 시 null 반환
     */
    fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record?

    /**
     * [GenericRecord]를 Avro 바이너리로 직렬화한 뒤, Base64로 인코딩된 문자열로 반환합니다.
     *
     * 네트워크 전송이나 텍스트 기반 저장소에 Avro 데이터를 저장할 때 유용합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param graph 직렬화할 Avro [GenericRecord] 객체
     * @return Base64로 인코딩된 문자열, [graph]가 null이면 null 반환
     */
    fun serializeAsString(schema: Schema, graph: GenericRecord?): String? {
        return graph?.run { serialize(schema, this)?.encodeBase64String() }
    }

    /**
     * Base64로 인코딩된 Avro 직렬화 문자열을 [GenericData.Record]로 역직렬화합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param avroText Base64로 인코딩된 직렬화 데이터
     * @return 역직렬화된 Avro [Record], [avroText]가 null이거나 실패 시 null 반환
     */
    fun deserializeFromString(schema: Schema, avroText: String?): GenericData.Record? {
        return avroText?.run { deserialize(schema, this.decodeBase64ByteArray()) }
    }
}
