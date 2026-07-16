package io.bluetape4k.avro.compat.issue754.kotlin

import io.bluetape4k.avro.AvroSpecificRecordSerializer
import org.apache.avro.specific.SpecificRecord

class LegacyAvroSpecificRecordImplementation: AvroSpecificRecordSerializer {
    override fun <T: SpecificRecord> serialize(graph: T?): ByteArray? =
        graph?.toString()?.encodeToByteArray()

    override fun <T: SpecificRecord> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? = null

    override fun <T: SpecificRecord> serializeList(collection: List<T>?): ByteArray? =
        collection?.takeIf { it.isNotEmpty() }?.toString()?.encodeToByteArray()

    override fun <T: SpecificRecord> deserializeList(avroBytes: ByteArray?, clazz: Class<T>): List<T> =
        emptyList()
}
