package io.bluetape4k.io.serializer

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty

/**
 * 압축을 지원하는 [BinarySerializer]
 *
 * ```
 * val serializer = CompressableBinarySerializer(JdkBinarySerializer(), GzipCompressor())
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
 * ```
 */
open class CompressableBinarySerializer(
    serializer: BinarySerializer,
    val compressor: Compressor,
): BinarySerializerDecorator(serializer) {

    companion object: KLogging()

    /**
     * I/O 직렬화에서 데이터를 직렬화하는 `serialize` 함수를 제공합니다.
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null)
            return emptyByteArray

        return compressor.compress(super.serialize(graph))
    }

    /**
     * I/O 직렬화에서 데이터를 역직렬화하는 `deserialize` 함수를 제공합니다.
     */
    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        if (bytes.isNullOrEmpty())
            return null
        return super.deserialize(compressor.decompress(bytes))
    }

    /**
     * I/O 직렬화 타입 변환을 위한 `toString` 함수를 제공합니다.
     */
    override fun toString(): String {
        return "CompressableBinarySerializer(compressor=${compressor.javaClass.simpleName}, serializer=${serializer.javaClass.simpleName})"
    }
}
