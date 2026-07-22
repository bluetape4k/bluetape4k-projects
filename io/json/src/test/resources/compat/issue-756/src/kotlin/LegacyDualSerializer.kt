package io.bluetape4k.json.compat.issue756.kotlin

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.json.JsonSerializer
import java.nio.ByteBuffer

interface LegacyBufferSerializer {
    fun serializeTo(graph: Any?, target: ByteBuffer): Int
}

class LegacyDualSerializer: BinarySerializer, JsonSerializer, LegacyBufferSerializer {
    override fun serialize(graph: Any?): ByteArray = graph?.toString()?.encodeToByteArray() ?: byteArrayOf()

    override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
        val bytes = serialize(graph)
        target.put(bytes)
        return bytes.size
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString() as T?

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString()?.let(clazz::cast)
}
