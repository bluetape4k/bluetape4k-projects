package io.bluetape4k.io.serializer.compat.issue756.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import io.bluetape4k.io.serializer.BinarySerializerDecorator;
import java.nio.charset.StandardCharsets;

public final class LegacyBinaryDecorator extends BinarySerializerDecorator {
    public LegacyBinaryDecorator(BinarySerializer serializer) {
        super(serializer);
    }

    @Override
    public byte[] serialize(Object graph) {
        String payload = new String(super.serialize(graph), StandardCharsets.UTF_8);
        return ("decorated:" + payload).getBytes(StandardCharsets.UTF_8);
    }
}
