package io.bluetape4k.avro.compat.issue754.kotlin

import io.bluetape4k.avro.AvroReflectSerializer

class LegacyAvroReflectImplementation: AvroReflectSerializer {
    override fun <T> serialize(graph: T?): ByteArray? = graph?.toString()?.encodeToByteArray()

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? =
        avroBytes?.takeIf { it.isNotEmpty() }?.decodeToString() as T?
}
