package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord

/**
 * Avro [GenericRecord]를 [ByteArray]로 직렬화하고, [GenericRecord]로 역직렬화를 수행합니다.
 *
 * ```
 * val serializer = DefaultGenericRecordSerializer()
 * val schema = ...
 * val record = ...
 * val bytes = serializer.serialize(schema, record)
 * val deserialized = serializer.deserialize(schema, bytes)
 * ```
 *
 * Avro Protocol을 이용하여, Data 전송, RCP Call 을 수행할 수 있습니다.
 * 데이터 전송 시, DefaultGenericRecordSerializer 가 avro instance 를
 * byte array 나 문자열로 변환하고, 수신하는 쪽에서 byte array 나 문자열를 Avro Instance 로 빌드할 수 있습니다
 */
interface AvroGenericRecordSerializer {

    /**
     * [graph]를 Avro로 직렬화를 수행합니다.
     *
     * @param graph 직렬화할 Avro 객체
     * @param schema Avro 객체의 [Schema] 정보
     * @return 직렬화된 [ByteArray], 실패 시에는 null을 반환
     */
    fun serialize(schema: Schema, graph: GenericRecord?): ByteArray?

    /**
     * Avro 직렬화된 정보를 Avro [Record]로 역직렬화합니다.
     *
     * @param avroBytes 직렬화된 데이터
     * @param schema Avro 객체의 [Schema] 정보
     * @return 역직렬화된 Avro [Record], 실패 시에는 null 반환
     */
    fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record?

    /**
     * [graph]를 Avro로 직렬화를 수행하여, base64 인코딩된 문자열로 반환합니다.
     *
     * @param graph 직렬화할 Avro 객체
     * @param schema Avro 객체의 [Schema] 정보
     * @return 직렬화된 정보를 Base64로 인코딩된 문자열, 실패 시에는 empty string
     */
    fun serializeAsString(schema: Schema, graph: GenericRecord?): String? {
        return graph?.run { serialize(schema, this)?.encodeBase64String() }
    }

    /**
     * base64로 인코딩된 Avro 직렬화 정보를 Avro [Record]로 역직렬화합니다.
     *
     * @param base64String 직렬화된 데이터
     * @param schema Avro 객체의 [Schema] 정보
     * @return 역직렬화된 Avro [Record], 실패 시에는 null 반환
     */
    fun deserializeFromString(schema: Schema, avroText: String?): GenericData.Record? {
        return avroText?.run { deserialize(schema, this.decodeBase64ByteArray()) }
    }
}
