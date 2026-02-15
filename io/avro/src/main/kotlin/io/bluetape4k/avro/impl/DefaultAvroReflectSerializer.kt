package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AvroReflectSerializer
import io.bluetape4k.avro.DEFAULT_CODEC_FACTORY
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.reflect.ReflectDatumWriter
import org.apache.avro.specific.SpecificDatumReader
import java.io.ByteArrayOutputStream

/**
 * [AvroReflectSerializer]의 기본 구현체입니다.
 *
 * Avro의 [ReflectDatumWriter]를 사용하여 Java/Kotlin 객체를 Reflection 기반으로 직렬화하고,
 * [SpecificDatumReader]를 사용하여 역직렬화합니다.
 * 코드 생성된 Avro 클래스뿐만 아니라, 일반 POJO/데이터 클래스도 직렬화할 수 있습니다.
 *
 * ```
 * val serializer = DefaultAvroReflectSerializer()
 *
 * val emp = TestMessageProvider.createEmployee()
 * val serialized = serializer.serialize(emp)
 * val deserialized = serializer.deserialize(serialized, Employee::class.java)
 * ```
 *
 * @property codecFactory Avro 직렬화 시 사용할 [CodecFactory] 인스턴스 (기본값: [DEFAULT_CODEC_FACTORY])
 * @see AvroReflectSerializer
 * @see DefaultAvroGenericRecordSerializer
 * @see DefaultAvroSpecificRecordSerializer
 */
class DefaultAvroReflectSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroReflectSerializer {

    companion object: KLogging() {
        /**
         * [DefaultAvroReflectSerializer] 인스턴스를 생성합니다.
         *
         * ```
         * // 기본 코덱(Zstandard 레벨 3) 사용
         * val serializer = DefaultAvroReflectSerializer()
         *
         * // 커스텀 코덱 사용
         * val snappySerializer = DefaultAvroReflectSerializer(CodecFactory.snappyCodec())
         * ```
         *
         * @param codecFactory 사용할 [CodecFactory] (기본값: [DEFAULT_CODEC_FACTORY])
         * @return [DefaultAvroReflectSerializer] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroReflectSerializer =
            DefaultAvroReflectSerializer(codecFactory)
    }

    /**
     * 객체를 Avro Reflection 기반으로 바이너리 형식으로 직렬화합니다.
     *
     * [ReflectDatumWriter]를 사용하여 객체의 필드 정보를 Reflection으로 분석한 뒤,
     * Avro 스키마를 자동 추론하여 직렬화합니다.
     *
     * @param T 직렬화할 객체의 타입
     * @param graph 직렬화할 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이거나 실패 시 null 반환
     */
    override fun <T> serialize(graph: T?): ByteArray? {
        if (graph == null) {
            return null
        }

        return try {
            val rdw = ReflectDatumWriter(graph.javaClass)
            DataFileWriter(rdw).setCodec(codecFactory).use { dfw ->
                ByteArrayOutputStream().use { bos ->
                    dfw.create(rdw.specificData.getSchema(graph.javaClass), bos)
                    dfw.append(graph)
                    dfw.flush()

                    bos.toByteArray()
                }
            }
        } catch (e: Throwable) {
            log.error(e) { "Reflect 기반 직렬화에 실패했습니다. graph=$graph" }
            null
        }
    }

    /**
     * Avro 바이너리 데이터를 Reflection 기반으로 지정된 타입의 인스턴스로 역직렬화합니다.
     *
     * [SpecificDatumReader]를 사용하여 [clazz] 타입으로 역직렬화합니다.
     * 스키마 진화(Schema Evolution)를 지원합니다.
     *
     * @param T 역직렬화할 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroBytes]가 null이거나 실패 시 null 반환
     */
    override fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? {
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
            log.error(e) { "Reflect 기반 역직렬화에 실패했습니다. clazz=${clazz.name}" }
            null
        }
    }
}
