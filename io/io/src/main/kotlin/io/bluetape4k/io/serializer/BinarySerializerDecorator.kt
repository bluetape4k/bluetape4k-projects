package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

/**
 * [BinarySerializer]를 Decorator pattern으로 사용하기 위한 클래스
 */
open class BinarySerializerDecorator(
    protected val serializer: BinarySerializer,
): BinarySerializer by serializer {

    companion object: KLogging()

}
