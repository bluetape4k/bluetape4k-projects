package io.bluetape4k.io.compressor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class CompressorByteBufferJavaContractTest {
    private final Compressor compressor = new ReversingFallbackCompressor();

    @Test
    void nullArgumentsFailBeforeBufferPreflight() {
        ByteBuffer readOnly = ByteBuffer.allocate(8).asReadOnlyBuffer();
        assertThrows(NullPointerException.class, () -> compressor.compress(null, readOnly));
        assertThrows(NullPointerException.class, () -> compressor.compress(ByteBuffer.allocate(1), null));
        assertThrows(NullPointerException.class, () -> compressor.decompress(null, null));
    }

    @Test
    void overflowPreservesCallerPositions() {
        ByteBuffer source = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
        ByteBuffer target = ByteBuffer.allocate(1);

        assertThrows(BufferOverflowException.class, () -> compressor.compress(source, target));

        assertEquals(0, source.position());
        assertEquals(0, target.position());
    }
}
