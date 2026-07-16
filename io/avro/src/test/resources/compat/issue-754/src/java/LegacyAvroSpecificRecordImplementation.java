package io.bluetape4k.avro.compat.issue754.java;

import io.bluetape4k.avro.AvroSpecificRecordSerializer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.avro.specific.SpecificRecord;

public final class LegacyAvroSpecificRecordImplementation implements AvroSpecificRecordSerializer {
    public int serializeCalls;
    public int deserializeCalls;
    public int serializeListCalls;
    public int deserializeListCalls;

    @Override
    public <T extends SpecificRecord> byte[] serialize(T graph) {
        serializeCalls++;
        return graph == null ? null : "specific".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T extends SpecificRecord> T deserialize(byte[] bytes, Class<T> clazz) {
        deserializeCalls++;
        return null;
    }

    @Override
    public <T extends SpecificRecord> byte[] serializeList(List<? extends T> collection) {
        serializeListCalls++;
        return collection == null || collection.isEmpty()
            ? null
            : "specific-list".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T extends SpecificRecord> List<T> deserializeList(byte[] bytes, Class<T> clazz) {
        deserializeListCalls++;
        return Collections.emptyList();
    }
}
