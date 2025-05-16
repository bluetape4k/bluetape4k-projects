package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging

class FuryBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()

    override val serializer: BinarySerializer = FuryBinarySerializer()

}
