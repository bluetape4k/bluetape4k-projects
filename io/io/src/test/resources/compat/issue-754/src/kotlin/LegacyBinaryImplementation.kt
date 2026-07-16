package io.bluetape4k.io.serializer.compat.issue754.kotlin

import io.bluetape4k.io.serializer.BinarySerializer

class LegacyBinaryImplementation: BinarySerializer {
    override fun serialize(graph: Any?): ByteArray = graph?.toString()?.encodeToByteArray() ?: byteArrayOf()

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString() as T?
}
