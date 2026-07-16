package io.bluetape4k.json.compat.issue754.java;

import io.bluetape4k.json.JsonSerializer;
import java.nio.charset.StandardCharsets;

public final class LegacyJsonImplementation implements JsonSerializer {
    public int serializeCalls;
    public int deserializeCalls;

    @Override
    public byte[] serialize(Object graph) {
        serializeCalls++;
        return graph == null ? new byte[0] : graph.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        deserializeCalls++;
        Object value = bytes == null || bytes.length == 0
            ? null
            : new String(bytes, StandardCharsets.UTF_8);
        return value == null ? null : (T) value;
    }
}
