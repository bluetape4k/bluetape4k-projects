package io.bluetape4k.io.compressor.abi.issue755.java;

import io.bluetape4k.io.compressor.Compressor;

public final class AmbiguousNullCaller {
    private AmbiguousNullCaller() {}

    public static void invoke(Compressor compressor) {
        compressor.compress(null);
    }
}
