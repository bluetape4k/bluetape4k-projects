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
 * Avro [SpecificRecord] 인스턴스를 [ByteArray]로 직렬화하고 역직렬화합니다.
 * [SpecificDatumWriter]와 [SpecificDatumReader]를 사용하여 타입 안전한 직렬화를 수행합니다.
 * 단일 객체 및 리스트의 직렬화/역직렬화를 모두 지원합니다.
 *
 * Avro Protocol을 이용하여 데이터 전송이나 RPC 호출에 활용할 수 있습니다.
 * 데이터 전송 시, Avro 인스턴스를 byte array나 문자열로 변환하고,
 * 수신 측에서 byte array나 문자열로부터 Avro 인스턴스를 복원합니다.
 *
 * ```
 * val serializer = DefaultAvroSpecificRecordSerializer()
 * val emp = TestMessageProvider.createEmployee()
 *
 * // 단일 객체 직렬화/역직렬화
 * val serialized = serializer.serialize(emp)
 * val deserialized = serializer.deserialize(serialized, Employee::class.java)
 *
 * // 리스트 직렬화/역직렬화
 * val employees = listOf(emp1, emp2, emp3)
 * val listBytes = serializer.serializeList(employees)
 * val deserializedList = serializer.deserializeList(listBytes, Employee::class.java)
 * ```
 *
 * @property codecFactory Avro 직렬화 시 사용할 [CodecFactory] 인스턴스 (기본값: [DEFAULT_CODEC_FACTORY])
 * @see AvroSpecificRecordSerializer
 * @see DefaultAvroGenericRecordSerializer
 * @see DefaultAvroReflectSerializer
 */
class DefaultAvroSpecificRecordSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroSpecificRecordSerializer {

    companion object: KLogging() {
        /**
         * [DefaultAvroSpecificRecordSerializer] 인스턴스를 생성합니다.
         *
         * ```
         * // 기본 코덱(Zstandard 레벨 3) 사용
         * val serializer = DefaultAvroSpecificRecordSerializer()
         *
         * // 커스텀 코덱 사용
         * val snappySerializer = DefaultAvroSpecificRecordSerializer(CodecFactory.snappyCodec())
         * ```
         *
         * @param codecFactory 사용할 [CodecFactory] (기본값: [DEFAULT_CODEC_FACTORY])
         * @return [DefaultAvroSpecificRecordSerializer] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroSpecificRecordSerializer {
            return DefaultAvroSpecificRecordSerializer(codecFactory)
        }
    }

    /**
     * Avro [SpecificRecord] 인스턴스를 바이너리 형식으로 직렬화합니다.
     *
     * [SpecificDatumWriter]를 사용하여 [graph]의 스키마 정보에 따라 직렬화하고,
     * 설정된 [codecFactory]로 압축하여 [ByteArray]로 반환합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param graph 직렬화할 Avro [SpecificRecord] 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이거나 실패 시 null 반환
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
     * Avro [SpecificRecord] 인스턴스의 리스트를 바이너리 형식으로 직렬화합니다.
     *
     * 리스트의 첫 번째 요소에서 스키마 정보를 추출하여, 모든 요소를 하나의 DataFile로 직렬화합니다.
     * 배치 전송이나 대량 데이터 저장에 효율적입니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param collection 직렬화할 [SpecificRecord] 리스트
     * @return 직렬화된 [ByteArray], [collection]이 null이거나 비어있으면 null 반환
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
     * Avro 바이너리 데이터를 [SpecificRecord] 인스턴스로 역직렬화합니다.
     *
     * [SpecificDatumReader]를 사용하여 [clazz] 타입으로 역직렬화합니다.
     * 스키마 진화(Schema Evolution)를 지원하여, writer 스키마와 reader 스키마가 다르더라도
     * 호환 가능한 경우 정상적으로 역직렬화합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroBytes]가 null이거나 실패 시 null 반환
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
     * Avro 바이너리 데이터를 [SpecificRecord] 인스턴스의 리스트로 역직렬화합니다.
     *
     * DataFile에 포함된 모든 레코드를 순차적으로 읽어 리스트로 반환합니다.
     * Eclipse Collections의 FastList를 사용하여 고성능 리스트 생성을 수행합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 컬렉션 요소의 [Class] 정보
     * @return 역직렬화된 리스트, [avroBytes]가 null이거나 비어있으면 빈 리스트 반환
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
