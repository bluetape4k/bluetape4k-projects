package io.bluetape4k.json.compat.issue754.java;

import io.bluetape4k.json.JsonSerializer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class NewJsonBufferCaller {
    private NewJsonBufferCaller() {
    }

    public static void main(String[] args) throws Exception {
        LegacyJsonImplementation serializer = new LegacyJsonImplementation();

        expectNullPointer(() -> serializer.serializeTo("json", null));
        require(serializer.serializeCalls == 0, "null target invoked the legacy serializer");
        expectNullPointer(() -> serializer.deserializeFrom(null, String.class));
        require(serializer.deserializeCalls == 0, "null source invoked the legacy deserializer");

        verifyDefaults(serializer);
        JsonSerializer kotlinSerializer = (JsonSerializer) Class.forName(
            "io.bluetape4k.json.compat.issue754.kotlin.LegacyJsonImplementation"
        ).getDeclaredConstructor().newInstance();
        verifyDefaults(kotlinSerializer);

        System.out.println("json-default-dispatch=PASS");
    }

    private static void verifyDefaults(JsonSerializer serializer) {
        ByteBuffer target = ByteBuffer.allocate(24);
        target.position(4);
        int count = serializer.serializeTo("json", target);
        require(count == 4, "unexpected JSON write count");
        require(target.position() == 8, "target position did not advance by the write count");

        ByteBuffer source = ByteBuffer.wrap("json".getBytes(StandardCharsets.UTF_8));
        int sourcePosition = source.position();
        String restored = serializer.deserializeFrom(source, String.class);
        require("json".equals(restored), "unexpected JSON restored value");
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
