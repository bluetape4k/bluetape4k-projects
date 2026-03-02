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
 * ## 동작/계약
 * - `GenericDatumWriter/Reader`를 사용해 스키마 기반 DataFile 직렬화를 수행합니다.
 * - [serialize]/[deserialize] 입력이 `null`이면 `null`을 반환합니다.
 * - 실패 시 로그를 남기고 `null`을 반환합니다.
 *
 * ```kotlin
 * val schema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL)
 * val bytes = DefaultAvroGenericRecordSerializer().serialize(schema, null)
 * // bytes == null
 * ```
 */
class DefaultAvroGenericRecordSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroGenericRecordSerializer {

    companion object: KLogging() {
        /**
         * 코덱을 지정해 [DefaultAvroGenericRecordSerializer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [codecFactory]를 DataFileWriter 코덱 설정으로 사용합니다.
         * - 생략 시 [DEFAULT_CODEC_FACTORY]를 사용합니다.
         *
         * ```kotlin
         * val serializer = DefaultAvroGenericRecordSerializer()
         * // serializer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroGenericRecordSerializer {
            return DefaultAvroGenericRecordSerializer(codecFactory)
        }
    }

    /**
     * `GenericRecord`를 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - [schema] 기준으로 DataFile을 생성하고 새 바이트 배열을 반환합니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
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
     * Avro 바이트 배열을 [GenericData.Record]로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - DataFile에서 첫 레코드 1건만 읽어 반환합니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
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
