package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty

/**
 * [BinarySerializer]의 최상위 추상화 클래스입니다.
 *
 * ## Null/Empty 처리 정책
 * - `serialize(null)`: 빈 [ByteArray]를 반환합니다.
 * - `deserialize(null)` 또는 `deserialize(emptyByteArray)`: `null`을 반환합니다.
 * - 직렬화/역직렬화 실패 시: [BinarySerializationException]을 던집니다.
 *
 * ## 구현 방법
 * [doSerialize]와 [doDeserialize]를 구현하면 됩니다.
 * null/empty 체크와 예외 처리는 이 클래스에서 담당합니다.
 *
 * ```kotlin
 * class MySerializer: AbstractBinarySerializer() {
 *     override fun doSerialize(graph: Any): ByteArray = /* 직렬화 구현 */
 *     override fun <T: Any> doDeserialize(bytes: ByteArray): T? = /* 역직렬화 구현 */
 * }
 * ```
 *
 * @see BinarySerializer
 * @see BinarySerializers
 */
abstract class AbstractBinarySerializer: BinarySerializer {

    companion object: KLogging()

    protected abstract fun doSerialize(graph: Any): ByteArray
    protected abstract fun <T: Any> doDeserialize(bytes: ByteArray): T?

    /**
     * 객체를 Binary 방식으로 직렬화합니다.
     *
     * 정책: 직렬화 실패 시 [BinarySerializationException] 예외를 던집니다.
     *
     * ```
     * val serializer = JdkBinarySerializer()
     * val bytes = serializer.serialize("Hello, World!")
     * ```
     *
     * @param graph 직렬화할 객체
     * @return 직렬화된 데이터
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null) {
            return emptyByteArray
        }

        return try {
            doSerialize(graph)
        } catch (e: Throwable) {
            log.error(e) { "Fail to serialize. throw BinarySerializationException. graph=$graph" }
            throw BinarySerializationException("Fail to serialize. graphType=${graph.javaClass.name}", e)
        }
    }

    /**
     * 직렬화된 데이터를 읽어 대상 객체로 역직렬화합니다.
     *
     * 정책: 역직렬화 실패 시 [BinarySerializationException] 예외를 던집니다.
     *
     * ```
     * val serializer = JdkBinarySerializer()
     * val bytes = serializer.serialize("Hello, World!")
     * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
     * ```
     *
     * @param T     역직렬화할 객체 수형
     * @param bytes 직렬화된 데이터
     * @return 역직렬화한 객체
     */
    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        if (bytes.isNullOrEmpty()) {
            return null
        }
        val source = bytes!!

        return try {
            doDeserialize(source)
        } catch (e: Throwable) {
            log.error(e) { "Fail to deserialize. throw BinarySerializationException." }
            throw BinarySerializationException("Fail to deserialize. bytesSize=${source.size}", e)
        }
    }
}
