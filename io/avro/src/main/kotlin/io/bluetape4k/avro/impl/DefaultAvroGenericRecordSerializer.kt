package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AvroGenericRecordSerializer
import io.bluetape4k.avro.DEFAULT_CODEC_FACTORY
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import org.apache.avro.Schema
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import java.io.ByteArrayOutputStream

/**
 * [AvroGenericRecordSerializer]의 기본 구현체입니다.
 *
 * Avro [GenericRecord]를 [ByteArray]로 직렬화하고, [ByteArray]에서 [GenericData.Record]로 역직렬화합니다.
 * [GenericDatumWriter]와 [GenericDatumReader]를 사용하여 스키마 기반의 범용 직렬화를 수행합니다.
 *
 * ```
 * val serializer = DefaultAvroGenericRecordSerializer()
 *
 * val emp = TestMessageProvider.createEmployee()
 * val schema = Employee.getClassSchema()
 *
 * val serialized = serializer.serialize(schema, emp)
 * val deserialized = serializer.deserialize(schema, serialized)
 * ```
 *
 * @property codecFactory Avro 직렬화 시 사용할 [CodecFactory] 인스턴스 (기본값: [DEFAULT_CODEC_FACTORY])
 * @see AvroGenericRecordSerializer
 * @see DefaultAvroSpecificRecordSerializer
 * @see DefaultAvroReflectSerializer
 */
class DefaultAvroGenericRecordSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroGenericRecordSerializer {

    companion object: KLogging() {
        /**
         * [DefaultAvroGenericRecordSerializer] 인스턴스를 생성합니다.
         *
         * ```
         * // 기본 코덱(Zstandard 레벨 3) 사용
         * val serializer = DefaultAvroGenericRecordSerializer()
         *
         * // 커스텀 코덱 사용
         * val snappySerializer = DefaultAvroGenericRecordSerializer(CodecFactory.snappyCodec())
         * ```
         *
         * @param codecFactory 사용할 [CodecFactory] (기본값: [DEFAULT_CODEC_FACTORY])
         * @return [DefaultAvroGenericRecordSerializer] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroGenericRecordSerializer {
            return DefaultAvroGenericRecordSerializer(codecFactory)
        }
    }

    /**
     * [GenericRecord]를 Avro 바이너리 형식으로 직렬화합니다.
     *
     * [GenericDatumWriter]를 사용하여 [schema] 정보에 따라 [graph]를 직렬화하고,
     * 설정된 [codecFactory]로 압축하여 [ByteArray]로 반환합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param graph 직렬화할 Avro [GenericRecord] 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이거나 실패 시 null 반환
     */
    override fun serialize(schema: Schema, graph: GenericRecord?): ByteArray? {
        if (graph == null) {
            return null
        }

        return try {
            val datumWriter = GenericDatumWriter<GenericRecord>(schema)
            DataFileWriter(datumWriter).setCodec(codecFactory).use { dfw ->
                ByteArrayOutputStream().use { bos ->
                    dfw.create(schema, bos)
                    dfw.append(graph)
                    dfw.flush()

                    bos.toByteArray()
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "GenericRecord 직렬화에 실패했습니다. schema=${schema.name}" }
            null
        }
    }

    /**
     * Avro 바이너리 데이터를 [GenericData.Record]로 역직렬화합니다.
     *
     * [GenericDatumReader]를 사용하여 [schema] 정보에 따라 [avroBytes]를 역직렬화합니다.
     *
     * @param schema Avro 객체의 [Schema] 정보
     * @param avroBytes 직렬화된 데이터
     * @return 역직렬화된 Avro [GenericData.Record], [avroBytes]가 null이거나 실패 시 null 반환
     */
    override fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record? {
        if (avroBytes == null) {
            return null
        }

        return try {
            SeekableByteArrayInput(avroBytes).use { sin ->
                val datumReader = GenericDatumReader<GenericData.Record>(schema)
                DataFileReader(sin, datumReader).use { dfr ->
                    if (dfr.hasNext()) dfr.next()
                    else null
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "GenericRecord 역직렬화에 실패했습니다. schema=${schema.name}" }
            null
        }
    }
}
