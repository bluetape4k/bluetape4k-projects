package io.bluetape4k.json.compat.issue756.java;

import io.bluetape4k.json.JsonSerializer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public final class LegacyJsonStreamCaller {
    private LegacyJsonStreamCaller() {
    }

    public static void main(String[] args) throws Exception {
        JsonSerializer serializer = new LegacyJsonImplementation();
        require("json".equals(new String(serializer.serialize("json"), StandardCharsets.UTF_8)),
            "legacy JSON call changed");

        Method method = JsonSerializer.class.getMethod(
            "serializeJsonToStream",
            Object.class,
            OutputStream.class
        );
        require(method.isDefault(), "JSON stream method is not a default");

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        Object written = method.invoke(serializer, "json", target);
        require(Integer.valueOf(4).equals(written), "unexpected JSON stream write count");
        require("json".equals(target.toString(StandardCharsets.UTF_8)), "unexpected JSON stream payload");
        System.out.println("legacy-json-java-stream-caller=PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
