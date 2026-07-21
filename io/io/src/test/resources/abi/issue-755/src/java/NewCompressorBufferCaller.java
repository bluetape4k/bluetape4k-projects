package io.bluetape4k.io.compressor.abi.issue755.java;

import io.bluetape4k.io.compressor.Compressor;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class NewCompressorBufferCaller {
    private NewCompressorBufferCaller() {}

    public static void main(String[] args) {
        Compressor compressor = new LegacyCompressorImplementation();
        byte[] payload = new byte[] {1, 2, 3, 4};
        ByteBuffer wire = ByteBuffer.allocate(payload.length);
        int compressed = compressor.compress(ByteBuffer.wrap(payload), wire);
        wire.flip();
        ByteBuffer plain = ByteBuffer.allocate(payload.length);
        int decompressed = compressor.decompress(wire, plain);
        if (compressed != payload.length || decompressed != payload.length) {
            throw new AssertionError("unexpected caller-owned byte count");
        }
        if (!Arrays.equals(payload, Arrays.copyOf(plain.array(), decompressed))) {
            throw new AssertionError("new caller-owned roundtrip failed");
        }
        System.out.println("new-java=PASS");
    }
}
