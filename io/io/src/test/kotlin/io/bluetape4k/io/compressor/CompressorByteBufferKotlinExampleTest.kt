package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

class CompressorByteBufferKotlinExampleTest {

    @Test
    fun `caller grows a direct target without consuming the source`() {
        val payload = ByteArray(4 * 1024) { index -> (index and 0x7F).toByte() }
        val source = ByteBuffer.wrap(payload)
        val wire = compressGrowing(Compressors.LZ4, source)
        val restored = ByteBuffer.allocateDirect(payload.size)

        Compressors.LZ4.decompress(wire, restored)
        restored.flip()

        source.position() shouldBeEqualTo 0
        ByteArray(restored.remaining()).also(restored::get) shouldContentEqual payload
    }

    private fun compressGrowing(compressor: Compressor, source: ByteBuffer): ByteBuffer {
        var capacity = 32
        while (capacity <= MAX_WIRE_SIZE) {
            val target = ByteBuffer.allocateDirect(capacity)
            try {
                compressor.compress(source, target)
                return target.apply { flip() }
            } catch (_: BufferOverflowException) {
                capacity *= 2
            }
        }
        throw BufferOverflowException()
    }

    companion object {
        private const val MAX_WIRE_SIZE = 64 * 1024
    }
}
