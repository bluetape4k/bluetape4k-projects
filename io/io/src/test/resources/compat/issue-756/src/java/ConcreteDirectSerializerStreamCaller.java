package io.bluetape4k.io.serializer.compat.issue756.java;

import io.bluetape4k.io.serializer.JdkBinarySerializer;
import io.bluetape4k.io.serializer.KryoBinarySerializer;
import io.bluetape4k.jackson.JacksonSerializer;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class ConcreteDirectSerializerStreamCaller {
    private ConcreteDirectSerializerStreamCaller() {
    }

    public static void main(String[] args) throws Exception {
        verifyJdk(new JdkBinarySerializer());
        verifyKryo(new KryoBinarySerializer());
        verifyJackson2(new JacksonSerializer());
        verifyJackson3(new io.bluetape4k.jackson3.JacksonSerializer());
        System.out.println("concrete-serializer-stream-callers=PASS");
    }

    private static void verifyJdk(JdkBinarySerializer serializer) throws Exception {
        verifyBinary(serializer.serialize("jdk"), target -> serializer.serializeBinaryToStream("jdk", target));
    }

    private static void verifyKryo(KryoBinarySerializer serializer) throws Exception {
        verifyBinary(serializer.serialize("kryo"), target -> serializer.serializeBinaryToStream("kryo", target));
    }

    private static void verifyJackson2(JacksonSerializer serializer) throws Exception {
        verifyBinary(serializer.serialize("jackson2"), target -> serializer.serializeJsonToStream("jackson2", target));
    }

    private static void verifyJackson3(io.bluetape4k.jackson3.JacksonSerializer serializer) throws Exception {
        verifyBinary(serializer.serialize("jackson3"), target -> serializer.serializeJsonToStream("jackson3", target));
    }

    private static void verifyBinary(byte[] expected, StreamWriter writer) throws Exception {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        int written = writer.write(target);
        byte[] actual = target.toByteArray();
        require(written == actual.length, "stream count does not match the target size");
        require(Arrays.equals(expected, actual),
            "stream payload does not match the existing serializer wire: " + new String(actual, StandardCharsets.UTF_8));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface StreamWriter {
        int write(ByteArrayOutputStream target) throws Exception;
    }
}
