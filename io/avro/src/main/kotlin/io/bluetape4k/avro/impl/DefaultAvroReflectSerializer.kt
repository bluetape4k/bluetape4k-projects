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
 * ## 동작/계약
 * - `ReflectDatumWriter`로 writer schema를 만들고 DataFile 형식으로 기록합니다.
 * - [serialize]/[deserialize] 입력이 `null`이면 `null`을 반환합니다.
 * - 직렬화/역직렬화 실패는 로그를 남기고 `null`을 반환합니다.
 *
 * ```kotlin
 * val serializer = DefaultAvroReflectSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes != null
 * ```
 */
class DefaultAvroReflectSerializer private constructor(
    private val codecFactory: CodecFactory,
): AvroReflectSerializer {

    companion object: KLogging() {
        /**
         * 코덱 설정을 지정해 [DefaultAvroReflectSerializer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [codecFactory]를 내부 DataFileWriter 설정에 그대로 사용합니다.
         * - [codecFactory]를 지정하지 않으면 [DEFAULT_CODEC_FACTORY]를 사용합니다.
         *
         * ```kotlin
         * val serializer = DefaultAvroReflectSerializer()
         * // serializer != null
         * ```
         */
        @JvmStatic
        operator fun invoke(
            codecFactory: CodecFactory = DEFAULT_CODEC_FACTORY,
        ): DefaultAvroReflectSerializer =
            DefaultAvroReflectSerializer(codecFactory)
    }

    /**
     * 객체를 Reflection 기반 Avro 바이트 배열로 직렬화합니다.
     *
     * ## 동작/계약
     * - [graph]가 `null`이면 `null`을 반환합니다.
     * - 직렬화 결과는 새 바이트 배열로 반환되며 입력 객체는 변경하지 않습니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
     *
     * ```kotlin
     * val bytes = DefaultAvroReflectSerializer().serialize(mapOf("id" to 1))
     * // bytes != null
     * ```
     *
     * @param graph 직렬화할 객체입니다. `null`이면 `null`을 반환합니다.
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
     * Avro 바이트 배열을 지정 타입으로 역직렬화합니다.
     *
     * ## 동작/계약
     * - [avroBytes]가 `null`이면 `null`을 반환합니다.
     * - DataFile에서 첫 레코드 1건만 읽어 반환합니다.
     * - 실패 시 로그를 남기고 `null`을 반환합니다.
     *
     * ```kotlin
     * val restored = DefaultAvroReflectSerializer().deserialize(null, Map::class.java)
     * // restored == null
     * ```
     *
     * @param avroBytes Avro 바이트 배열입니다. `null`이면 `null`을 반환합니다.
     * @param clazz 역직렬화 대상 클래스입니다.
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
