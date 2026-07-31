package io.bluetape4k.io.compressor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class CompressorByteBufferJavaExampleTest {
    private static final int MAX_WIRE_SIZE = 64 * 1024;

    @Test
    void callerGrowsADirectTargetWithoutConsumingTheSource() {
        byte[] payload = new byte[4 * 1024];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index & 0x7F);
        }
        ByteBuffer source = ByteBuffer.wrap(payload);
        ByteBuffer wire = compressGrowing(Compressors.INSTANCE.getLZ4(), source);
        ByteBuffer restored = ByteBuffer.allocateDirect(payload.length);

        Compressors.INSTANCE.getLZ4().decompress(wire, restored);
        restored.flip();
        byte[] actual = new byte[restored.remaining()];
        restored.get(actual);

        assertEquals(0, source.position());
        assertArrayEquals(payload, actual);
    }

    private static ByteBuffer compressGrowing(Compressor compressor, ByteBuffer source) {
        for (int capacity = 32; capacity <= MAX_WIRE_SIZE; capacity *= 2) {
            ByteBuffer target = ByteBuffer.allocateDirect(capacity);
            try {
                compressor.compress(source, target);
                return target.flip();
            } catch (BufferOverflowException ignored) {
                // The contract preserves source and target positions, so a larger target is safe.
            }
        }
        throw new BufferOverflowException();
    }
}
