package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

class JdkBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()
    
    override val serializer: BinarySerializer = JdkBinarySerializer()

}
