package io.bluetape4k.io.serializer

class JdkBinarySerializerTest: AbstractBinarySerializerTest() {

    override val serializer: BinarySerializer = JdkBinarySerializer()

}
