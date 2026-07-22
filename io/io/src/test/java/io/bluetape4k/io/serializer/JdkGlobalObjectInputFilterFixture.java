package io.bluetape4k.io.serializer;

import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class JdkGlobalObjectInputFilterFixture {

    private JdkGlobalObjectInputFilterFixture() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one mode argument.");
        }
        if (ObjectInputFilter.Config.getSerialFilter() == null) {
            throw new IllegalStateException("Global serial filter was not installed at JVM startup.");
        }

        var serializer = new JdkBinarySerializer(4096, null);
        var expected = new Payload("global-filter");
        var wire = serializer.serialize(expected);
        var source = boundedDirectSource(wire);

        switch (args[0]) {
            case "allow" -> {
                Payload fromArray = serializer.deserialize(wire);
                Payload fromDirect = serializer.deserializeFrom(source);
                if (!expected.equals(fromArray) || !expected.equals(fromDirect)) {
                    throw new AssertionError("Global allow filter decode parity failed.");
                }
                assertSourceState(source, wire.length);
                System.out.println("GLOBAL_FILTER_ALLOW_PASS");
            }
            case "reject" -> {
                var arrayFailure = captureFailure(() -> serializer.deserialize(wire));
                var directFailure = captureFailure(() -> serializer.deserializeFrom(source));
                assertRejected(arrayFailure);
                assertRejected(directFailure);
                if (!arrayFailure.getClass().equals(directFailure.getClass()) ||
                    !arrayFailure.getCause().getClass().equals(directFailure.getCause().getClass())) {
                    throw new AssertionError("Global reject filter failure parity failed.");
                }
                assertSourceState(source, wire.length);
                System.out.println("GLOBAL_FILTER_REJECT_PASS");
            }
            default -> throw new IllegalArgumentException("Unknown mode: " + args[0]);
        }
    }

    private static ByteBuffer boundedDirectSource(byte[] wire) {
        var source = ByteBuffer.allocateDirect(wire.length + 4);
        source.put((byte) 0x51);
        source.put((byte) 0x52);
        source.put(wire);
        source.put((byte) 0x53);
        source.put((byte) 0x54);
        source.position(2);
        source.limit(2 + wire.length);
        source.order(ByteOrder.LITTLE_ENDIAN);
        source.mark();
        return source;
    }

    private static void assertSourceState(ByteBuffer source, int wireLength) {
        if (source.position() != 2 || source.limit() != 2 + wireLength ||
            source.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new AssertionError("Source state changed during bounded direct decode.");
        }
        source.reset();
        if (source.position() != 2) {
            throw new AssertionError("Source mark changed during bounded direct decode.");
        }
    }

    private static Throwable captureFailure(ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable failure) {
            return failure;
        }
        throw new AssertionError("Expected decode failure.");
    }

    private static void assertRejected(Throwable failure) {
        if (!(failure instanceof BinarySerializationException) ||
            !(failure.getCause() instanceof InvalidClassException)) {
            throw new AssertionError("Expected BinarySerializationException caused by InvalidClassException.", failure);
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run();
    }

    private record Payload(String value) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
