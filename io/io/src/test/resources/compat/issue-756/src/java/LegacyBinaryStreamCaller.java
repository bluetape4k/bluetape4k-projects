package io.bluetape4k.io.serializer.compat.issue756.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public final class LegacyBinaryStreamCaller {
    private LegacyBinaryStreamCaller() {
    }

    public static void main(String[] args) throws Exception {
        BinarySerializer serializer = new LegacyBinaryImplementation();
        require("binary".equals(new String(serializer.serialize("binary"), StandardCharsets.UTF_8)),
            "legacy binary call changed");

        Method method = BinarySerializer.class.getMethod(
            "serializeBinaryToStream",
            Object.class,
            OutputStream.class
        );
        require(method.isDefault(), "binary stream method is not a default");

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        Object written = method.invoke(serializer, "binary", target);
        require(Integer.valueOf(6).equals(written), "unexpected binary stream write count");
        require("binary".equals(target.toString(StandardCharsets.UTF_8)), "unexpected binary stream payload");
        System.out.println("legacy-binary-java-stream-caller=PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
