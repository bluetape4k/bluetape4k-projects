package io.bluetape4k.io.serializer

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.support.emptyByteArray

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

    override fun serialize(graph: Any?): ByteArray {
        return graph?.run { compressor.compress(super.serialize(graph)) } ?: emptyByteArray
    }

    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        return bytes?.run { super.deserialize(compressor.decompress(bytes)) }
    }

    override fun toString(): String {
        return "CompressableBinarySerializer(compressor=${compressor.javaClass.simpleName}, serializer=${serializer.javaClass.simpleName})"
    }
}
