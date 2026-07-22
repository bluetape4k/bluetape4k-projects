package io.bluetape4k.io.serializer.compat.issue756.java;

import io.bluetape4k.io.serializer.BinarySerializer;
import io.bluetape4k.json.JsonSerializer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class ConcreteSerializerStreamCaller {
    private static final String JAVA_DUAL =
        "io.bluetape4k.json.compat.issue756.java.LegacyDualSerializer";
    private static final String KOTLIN_DUAL =
        "io.bluetape4k.json.compat.issue756.kotlin.LegacyDualSerializer";
    private static final String JAVA_DECORATOR =
        "io.bluetape4k.io.serializer.compat.issue756.java.LegacyBinaryDecorator";
    private static final String KOTLIN_DECORATOR =
        "io.bluetape4k.io.serializer.compat.issue756.kotlin.LegacyBinaryDecorator";

    private ConcreteSerializerStreamCaller() {
    }

    public static void main(String[] args) throws Exception {
        verifyUnambiguousNullCalls();
        if (args.length == 1 && "decorator".equals(args[0])) {
            verifyLegacyDecorator(JAVA_DECORATOR);
            verifyLegacyDecorator(KOTLIN_DECORATOR);
            System.out.println("legacy-binary-decorator-semantics=PASS");
            return;
        }
        if (args.length != 0 && !(args.length == 1 && "dual".equals(args[0]))) {
            throw new AssertionError("expected no argument, dual, or decorator");
        }
        verifyDual(JAVA_DUAL);
        verifyDual(KOTLIN_DUAL);
        System.out.println("dual-stream-default-dispatch=PASS");
    }

    private static void verifyUnambiguousNullCalls() throws Exception {
        PlainDualSerializer serializer = new PlainDualSerializer();
        BinarySerializer binary = serializer;
        JsonSerializer json = serializer;
        expectNullPointer(() -> binary.serializeBinaryToStream("binary", null));
        expectNullPointer(() -> json.serializeJsonToStream("json", null));
    }

    private static void verifyDual(String className) throws Exception {
        Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
        BinarySerializer binary = (BinarySerializer) instance;
        JsonSerializer json = (JsonSerializer) instance;

        Method binaryMethod = BinarySerializer.class.getMethod(
            "serializeBinaryToStream",
            Object.class,
            OutputStream.class
        );
        Method jsonMethod = JsonSerializer.class.getMethod(
            "serializeJsonToStream",
            Object.class,
            OutputStream.class
        );
        require(binaryMethod.isDefault(), "binary stream method is not a default");
        require(jsonMethod.isDefault(), "JSON stream method is not a default");

        ByteArrayOutputStream binaryTarget = new ByteArrayOutputStream();
        ByteArrayOutputStream jsonTarget = new ByteArrayOutputStream();
        Object binaryCount = binaryMethod.invoke(binary, "binary", binaryTarget);
        Object jsonCount = jsonMethod.invoke(json, "json", jsonTarget);
        require(Integer.valueOf(6).equals(binaryCount), "unexpected binary default count");
        require(Integer.valueOf(4).equals(jsonCount), "unexpected JSON default count");
        require("binary".equals(binaryTarget.toString(StandardCharsets.UTF_8)), "unexpected binary default payload");
        require("json".equals(jsonTarget.toString(StandardCharsets.UTF_8)), "unexpected JSON default payload");
    }

    private static void verifyLegacyDecorator(String className) throws Exception {
        Constructor<?> constructor = Class.forName(className).getConstructor(BinarySerializer.class);
        BinarySerializer decorator = (BinarySerializer) constructor.newInstance(new PlainDualSerializer());
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        int written = decorator.serializeBinaryToStream("value", target);
        require(written == 15, "unexpected decorated stream count");
        require("decorated:value".equals(target.toString(StandardCharsets.UTF_8)),
            "stream dispatch bypassed the legacy decorator override");
    }

    private static void expectNullPointer(CheckedRunnable action) throws Exception {
        try {
            action.run();
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

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class PlainDualSerializer implements BinarySerializer, JsonSerializer {
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
}
