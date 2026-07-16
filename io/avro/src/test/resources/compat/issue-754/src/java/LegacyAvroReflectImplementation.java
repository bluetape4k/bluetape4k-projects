package io.bluetape4k.avro.compat.issue754.java;

import io.bluetape4k.avro.AvroReflectSerializer;
import java.nio.charset.StandardCharsets;

public final class LegacyAvroReflectImplementation implements AvroReflectSerializer {
    public int serializeCalls;
    public int deserializeCalls;

    @Override
    public <T> byte[] serialize(T graph) {
        serializeCalls++;
        return graph == null ? null : "reflect".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        deserializeCalls++;
        return bytes == null || bytes.length == 0 ? null : clazz.cast("reflect");
    }
}
