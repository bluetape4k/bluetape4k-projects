package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty

/**
 * [BinarySerializer]의 최상위 추상화 클래스
 */
abstract class AbstractBinarySerializer: BinarySerializer {

    companion object: KLogging()

    protected abstract fun doSerialize(graph: Any): ByteArray
    protected abstract fun <T: Any> doDeserialize(bytes: ByteArray): T?

    /**
     * 객체를 Binary 방식으로 직렬화합니다.
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
            log.error(e) { "Fail to serialize. graph=$graph" }
            emptyByteArray
        }
    }

    /**
     * 직렬화된 데이터를 읽어 대상 객체로 역직렬화합니다.
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

        return try {
            doDeserialize(bytes!!)
        } catch (e: Throwable) {
            log.error(e) { "Fail to deserialize." }
            null
        }
    }
}
