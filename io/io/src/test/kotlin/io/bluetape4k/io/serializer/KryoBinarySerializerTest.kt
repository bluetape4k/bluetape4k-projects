package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

class KryoBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()
    
    override val serializer: BinarySerializer = KryoBinarySerializer()

}
