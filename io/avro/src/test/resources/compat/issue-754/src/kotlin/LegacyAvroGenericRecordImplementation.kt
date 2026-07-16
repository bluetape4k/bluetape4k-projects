package io.bluetape4k.avro.compat.issue754.kotlin

import io.bluetape4k.avro.AvroGenericRecordSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord

class LegacyAvroGenericRecordImplementation: AvroGenericRecordSerializer {
    override fun serialize(schema: Schema, graph: GenericRecord?): ByteArray? =
        graph?.toString()?.encodeToByteArray()

    override fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record? = null
}
