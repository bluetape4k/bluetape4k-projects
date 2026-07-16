package io.bluetape4k.io.serializer.compat.issue754.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NewBinaryBufferCaller {
    private NewBinaryBufferCaller() {
    }

    public static void main(String[] args) throws Exception {
        LegacyBinaryImplementation serializer = new LegacyBinaryImplementation();

        expectNullPointer(() -> serializer.serializeTo("binary", null));
        require(serializer.serializeCalls == 0, "null target invoked the legacy serializer");
        expectNullPointer(() -> serializer.deserializeFrom(null));
        require(serializer.deserializeCalls == 0, "null source invoked the legacy deserializer");

        verifyDefaults(serializer);
        BinarySerializer kotlinSerializer = (BinarySerializer) Class.forName(
            "io.bluetape4k.io.serializer.compat.issue754.kotlin.LegacyBinaryImplementation"
        ).getDeclaredConstructor().newInstance();
        verifyDefaults(kotlinSerializer);

        System.out.println("binary-default-dispatch=PASS");
    }

    private static void verifyDefaults(BinarySerializer serializer) {
        ByteBuffer target = ByteBuffer.allocate(32);
        target.position(3);
        int count = serializer.serializeTo("binary", target);
        require(count == 6, "unexpected binary write count");
        require(target.position() == 9, "target position did not advance by the write count");

        ByteBuffer source = ByteBuffer.wrap("binary".getBytes(StandardCharsets.UTF_8));
        int sourcePosition = source.position();
        String restored = serializer.deserializeFrom(source);
        require("binary".equals(restored), "unexpected binary restored value");
        require(source.position() == sourcePosition, "source position changed");
    }

    private static void expectNullPointer(Runnable block) {
        try {
            block.run();
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
