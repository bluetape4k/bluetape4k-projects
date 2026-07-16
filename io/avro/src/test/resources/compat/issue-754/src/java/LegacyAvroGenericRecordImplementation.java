package io.bluetape4k.avro.compat.issue754.java;

import io.bluetape4k.avro.AvroGenericRecordSerializer;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

public final class LegacyAvroGenericRecordImplementation implements AvroGenericRecordSerializer {
    public int serializeCalls;
    public int deserializeCalls;

    @Override
    public byte[] serialize(Schema schema, GenericRecord graph) {
        serializeCalls++;
        return graph == null ? null : "generic".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public GenericData.Record deserialize(Schema schema, byte[] bytes) {
        deserializeCalls++;
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        GenericData.Record record = new GenericData.Record(schema);
        record.put("value", "generic");
        return record;
    }
}
