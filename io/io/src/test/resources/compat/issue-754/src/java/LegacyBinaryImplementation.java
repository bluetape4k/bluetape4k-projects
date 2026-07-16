package io.bluetape4k.io.serializer.compat.issue754.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import java.nio.charset.StandardCharsets;

public final class LegacyBinaryImplementation implements BinarySerializer {
    public int serializeCalls;
    public int deserializeCalls;

    @Override
    public byte[] serialize(Object graph) {
        serializeCalls++;
        return graph == null ? new byte[0] : graph.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        deserializeCalls++;
        return bytes == null || bytes.length == 0
            ? null
            : (T) new String(bytes, StandardCharsets.UTF_8);
    }
}
