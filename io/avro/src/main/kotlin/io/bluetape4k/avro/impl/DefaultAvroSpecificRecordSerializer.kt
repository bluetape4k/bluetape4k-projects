package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AvroSpecificRecordSerializer
import io.bluetape4k.avro.DEFAULT_CODEC_FACTORY
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.isNullOrEmpty
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificRecord
import java.io.ByteArrayOutputStream

/**
 * [AvroSpecificRecordSerializer]의 기본 구현체입니다.
 *
 * ## 동작/계약
 * - `SpecificDatumWriter/Reader` 기반으로 타입 안전한 DataFile 직렬화를 수행합니다.
 * - 단건/리스트 API 모두 실패 시 로그를 남기고 `null` 또는 빈 리스트를 반환합니다.
 * - [serialize]/[deserialize]는 `null` 입력을 그대로 `null` 결과로 처리합니다.
 * - 리스트 직렬화는 첫 요소 스키마를 기준으로 전체 레코드를 기록합니다.
 *
 * ```kotlin
 * val serializer = DefaultAvroSpecificRecordSerializer()
 * val restored = serializer.deserialize(null, org.apache.avro.specific.SpecificRecord::class.java)
 * // restored == null
 * ```
 */
class DefaultAvroSpecificRecordSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroSpecificRecordSerializer {

    companion object: KLogging() {
        /**
         * 코덱을 지정해 [DefaultAvroSpecificRecordSerializer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [codecFactory]를 DataFileWriter 코덱 설정으로 사용합니다.
         * - 생략 시 [DEFAULT_CODEC_FACTORY]를 사용합니다.
         *
         * ```kotlin
         * val serializer = DefaultAvroSpecificRecordSerializer()
         * // serializer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroSpecificRecordSerializer {
            return DefaultAvroSpecificRecordSerializer(codecFactory)
        }
    }

    /**
     * `SpecificRecord` 단건을 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 성공 시 새 바이트 배열을 할당해 반환합니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroSpecificRecordSerializer()
     *     .serialize(null as org.apache.avro.specific.SpecificRecord?)
     * // bytes == null
     * ```
     *
     * @param graph 직렬화할 레코드입니다. `null`이면 `null`을 반환합니다.
     */
    override fun <T: SpecificRecord> serialize(graph: T?): ByteArray? {
        if (graph == null) {
            return null
        }

        return try {
            val sdw = SpecificDatumWriter<T>(graph.schema)
            DataFileWriter(sdw).setCodec(codecFactory).use { dfw ->
                ByteArrayOutputStream().use { bos ->
                    dfw.create(graph.schema, bos)
                    dfw.append(graph)
                    dfw.flush()

                    bos.toByteArray()
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "SpecificRecord 직렬화에 실패했습니다. graph=$graph" }
            null
        }
    }

    /**
     * `SpecificRecord` 리스트를 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [collection]이 `null`/빈 리스트면 `null`을 반환합니다.
     * - 첫 요소 스키마를 DataFile 스키마로 사용합니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroSpecificRecordSerializer().serializeList(emptyList())
     * // bytes == null
     * ```
     *
     * @param collection 직렬화할 레코드 목록입니다.
     */
    override fun <T: SpecificRecord> serializeList(collection: List<T>?): ByteArray? {
        if (collection.isNullOrEmpty()) {
            return null
        }
        return try {
            val schema = collection.first().schema
            val sdw = SpecificDatumWriter<T>(schema)
            DataFileWriter(sdw).setCodec(codecFactory).use { dfw ->
                ByteArrayOutputStream().use { bos ->
                    dfw.create(schema, bos)
                    collection.forEach { dfw.append(it) }
                    dfw.flush()

                    bos.toByteArray()
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "SpecificRecord 리스트 직렬화에 실패했습니다. size=${collection.size}" }
            null
        }
    }

    /**
     * Avro 바이트 배열을 `SpecificRecord` 단건으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - DataFile에서 첫 레코드 1건만 읽습니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
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
    override fun <T: SpecificRecord> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? {
        if (avroBytes == null) {
            return null
        }

        return try {
            SeekableByteArrayInput(avroBytes).use { sin ->
                val sdr = SpecificDatumReader(clazz)
                DataFileReader(sin, sdr).use { dfr ->
                    if (dfr.hasNext()) dfr.next()
                    else null
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "SpecificRecord 역직렬화에 실패했습니다. clazz=${clazz.name}" }
            null
        }
    }

    /**
     * Avro 바이트 배열을 `SpecificRecord` 리스트로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`/빈 배열이면 빈 리스트를 반환합니다.
     * - DataFile의 모든 레코드를 끝까지 읽어 새 리스트에 담아 반환합니다.
     * - 실패 시 로그를 남기고 빈 리스트를 반환합니다.
     *
     * ```kotlin
     * val list = DefaultAvroSpecificRecordSerializer()
     *     .deserializeList(null, org.apache.avro.specific.SpecificRecord::class.java)
     * // list == emptyList<org.apache.avro.specific.SpecificRecord>()
     * ```
     *
     * @param avroBytes Avro 바이트 배열입니다. `null`/빈 배열이면 빈 리스트를 반환합니다.
     * @param clazz 리스트 요소 클래스입니다.
     */
    override fun <T: SpecificRecord> deserializeList(avroBytes: ByteArray?, clazz: Class<T>): List<T> {
        if (avroBytes.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val result = mutableListOf<T>()
            SeekableByteArrayInput(avroBytes).use { sin ->
                val sdr = SpecificDatumReader(clazz)
                DataFileReader(sin, sdr).use { dfr ->
                    while (dfr.hasNext()) {
                        result.add(dfr.next())
                    }
                }
            }
            result
        } catch (e: Throwable) {
            log.error(e) { "SpecificRecord 리스트 역직렬화에 실패했습니다. clazz=${clazz.name}" }
            emptyList()
        }
    }
}
