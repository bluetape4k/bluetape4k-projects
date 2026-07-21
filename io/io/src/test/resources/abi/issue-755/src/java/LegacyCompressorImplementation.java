package io.bluetape4k.io.compressor.abi.issue755.java;

import io.bluetape4k.io.compressor.Compressor;

public final class LegacyCompressorImplementation implements Compressor {
    @Override
    public byte[] compress(byte[] plain) {
        return reverse(plain);
    }

    @Override
    public byte[] decompress(byte[] compressed) {
        return reverse(compressed);
    }

    private static byte[] reverse(byte[] input) {
        if (input == null) {
            return new byte[0];
        }
        byte[] output = input.clone();
        for (int left = 0, right = output.length - 1; left < right; left++, right--) {
            byte value = output[left];
            output[left] = output[right];
            output[right] = value;
        }
        return output;
    }
}
