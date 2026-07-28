package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import java.io.IOException
import java.io.OutputStream

/**
 * [BinarySerializer]를 Decorator pattern으로 감싸기 위한 기반 클래스입니다.
 *
 * 기존 [BinarySerializer] 구현체에 기능을 추가할 때 상속하여 사용합니다.
 * 모든 메서드는 기본적으로 내부 [serializer]에 위임됩니다.
 *
 * ## 사용 예시
 * ```kotlin
 * class MetricsSerializer(
 *     delegate: BinarySerializer,
 * ): BinarySerializerDecorator(delegate) {
 *     override fun serialize(graph: Any?): ByteArray {
 *         val start = System.nanoTime()
 *         return super.serialize(graph).also {
 *             log.debug { "serialize time: ${System.nanoTime() - start}ns" }
 *         }
 *     }
 * }
 * ```
 *
 * @param serializer 위임할 [BinarySerializer] 구현체
 * @see CompressableBinarySerializer
 */
open class BinarySerializerDecorator(
    protected val serializer: BinarySerializer,
): BinarySerializer by serializer {

    companion object: KLogging()

    /**
     * 호출자 소유 [target]에 쓰기 전에 이 decorator의 virtual [serialize] 계약을 통해 직렬화합니다.
     *
     * 할당 기반 호환성 경로는 의도된 것입니다. 그렇지 않으면 Kotlin interface delegation이
     * serializer transforms supplied by subclasses of this decorator.
     */
    @Throws(IOException::class)
    override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
        val bytes = serialize(graph)
        target.write(bytes)
        return bytes.size
    }

}
