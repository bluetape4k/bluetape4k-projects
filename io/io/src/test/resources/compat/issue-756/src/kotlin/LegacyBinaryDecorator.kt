package io.bluetape4k.io.serializer.compat.issue756.kotlin

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializerDecorator

class LegacyBinaryDecorator(
    serializer: BinarySerializer,
): BinarySerializerDecorator(serializer) {
    override fun serialize(graph: Any?): ByteArray =
        "decorated:${super.serialize(graph).decodeToString()}".encodeToByteArray()
}
