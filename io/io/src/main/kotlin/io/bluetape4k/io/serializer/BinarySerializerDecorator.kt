package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

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

}
