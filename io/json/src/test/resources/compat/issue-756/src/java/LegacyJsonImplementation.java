package io.bluetape4k.json.compat.issue756.java;

import io.bluetape4k.json.JsonSerializer;
import java.nio.charset.StandardCharsets;

public final class LegacyJsonImplementation implements JsonSerializer {
    @Override
    public byte[] serialize(Object graph) {
        return graph == null ? new byte[0] : graph.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Object value = bytes == null || bytes.length == 0
            ? null
            : new String(bytes, StandardCharsets.UTF_8);
        return value == null ? null : clazz.cast(value);
    }
}
