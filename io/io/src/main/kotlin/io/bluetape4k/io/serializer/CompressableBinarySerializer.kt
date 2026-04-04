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
     * 객체를 직렬화한 후 [compressor]로 압축하여 [ByteArray]로 반환합니다.
     * `graph`가 null 이면 빈 [ByteArray]를 반환합니다.
     *
     * ```kotlin
     * val serializer = CompressableBinarySerializer(JdkBinarySerializer(), GzipCompressor())
     * val bytes = serializer.serialize("Hello, World!")
     * // bytes.size < "Hello, World!".toByteArray().size (압축 효과)
     * ```
     *
     * @param graph 직렬화할 객체. null 이면 빈 [ByteArray] 반환.
     * @return 직렬화 후 압축된 [ByteArray]
     */
    override fun serialize(graph: Any?): ByteArray {
        if (graph == null)
            return emptyByteArray

        return compressor.compress(super.serialize(graph))
    }

    /**
     * [compressor]로 압축된 [bytes]를 압축 해제한 후 역직렬화하여 객체를 반환합니다.
     * [bytes]가 null 이거나 비어 있으면 null 을 반환합니다.
     *
     * ```kotlin
     * val serializer = CompressableBinarySerializer(JdkBinarySerializer(), GzipCompressor())
     * val bytes = serializer.serialize("Hello, World!")
     * val text = serializer.deserialize<String>(bytes)  // text="Hello, World!"
     * ```
     *
     * @param T 역직렬화할 객체 수형
     * @param bytes 압축된 직렬화 데이터. null 이거나 비어 있으면 null 반환.
     * @return 역직렬화한 객체, 또는 null
     */
    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        if (bytes.isNullOrEmpty())
            return null
        return super.deserialize(compressor.decompress(bytes))
    }

    /**
     * 직렬화기와 압축기 정보를 포함한 문자열 표현을 반환합니다.
     *
     * ```kotlin
     * val serializer = CompressableBinarySerializer(JdkBinarySerializer(), GzipCompressor())
     * println(serializer.toString())
     * // 출력: CompressableBinarySerializer(compressor=GzipCompressor, serializer=JdkBinarySerializer)
     * ```
     *
     * @return 직렬화기 설명 문자열
     */
    override fun toString(): String {
        return "CompressableBinarySerializer(compressor=${compressor.javaClass.simpleName}, serializer=${serializer.javaClass.simpleName})"
    }
}
