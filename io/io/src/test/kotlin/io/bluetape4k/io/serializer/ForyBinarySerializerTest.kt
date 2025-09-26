package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

class ForyBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()

    override val serializer: BinarySerializer = ForyBinarySerializer()

}
