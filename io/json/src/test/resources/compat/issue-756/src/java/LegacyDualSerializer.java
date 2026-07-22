package io.bluetape4k.json.compat.issue756.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import io.bluetape4k.json.JsonSerializer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

interface LegacyBufferSerializer {
    int serializeTo(Object graph, ByteBuffer target);
}

public final class LegacyDualSerializer implements BinarySerializer, JsonSerializer, LegacyBufferSerializer {
    @Override
    public byte[] serialize(Object graph) {
        return graph == null ? new byte[0] : graph.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int serializeTo(Object graph, ByteBuffer target) {
        byte[] bytes = serialize(graph);
        target.put(bytes);
        return bytes.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        return bytes == null || bytes.length == 0
            ? null
            : (T) new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Object value = deserialize(bytes);
        return value == null ? null : clazz.cast(value);
    }
}
