package io.bluetape4k.json.compat.issue754.kotlin

import io.bluetape4k.json.JsonSerializer

class LegacyJsonImplementation: JsonSerializer {
    override fun serialize(graph: Any?): ByteArray = graph?.toString()?.encodeToByteArray() ?: byteArrayOf()

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
        bytes?.takeIf { it.isNotEmpty() }?.decodeToString() as T?
}
