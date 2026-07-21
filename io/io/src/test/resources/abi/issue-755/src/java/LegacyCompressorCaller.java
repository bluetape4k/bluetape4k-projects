package io.bluetape4k.io.compressor.abi.issue755.java;

import io.bluetape4k.io.compressor.Compressor;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class LegacyCompressorCaller {
    private LegacyCompressorCaller() {}

    public static void main(String[] args) {
        Compressor compressor = new LegacyCompressorImplementation();
        byte[] payload = new byte[] {1, 2, 3, 4};
        byte[] wire = compressor.compress(payload);
        if (!Arrays.equals(payload, compressor.decompress(wire))) {
            throw new AssertionError("legacy byte array roundtrip failed");
        }
        ByteBuffer legacyWire = compressor.compress(ByteBuffer.wrap(payload));
        if (!Arrays.equals(payload, compressor.decompress(legacyWire).array())) {
            throw new AssertionError("legacy ByteBuffer roundtrip failed");
        }
        System.out.println("legacy-java=PASS");
    }
}
