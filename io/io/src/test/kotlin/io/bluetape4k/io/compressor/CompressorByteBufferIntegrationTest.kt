package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.util.stream.Stream

class CompressorByteBufferIntegrationTest {

    @ParameterizedTest(name = "{0} shared singleton")
    @MethodSource("sharedCompressors")
    fun `shared singleton supports concurrent caller-owned round trips`(
        name: String,
        compressor: Compressor,
    ) {
        MultithreadingTester()
            .workers(8)
            .rounds(4)
            .add {
                val payload = payloadFor(name)
                val compressed = ByteBuffer.allocateDirect(payload.size * 2)
                val compressedSize = compressor.compress(ByteBuffer.wrap(payload), compressed)
                val wire = compressed.duplicate().apply {
                    flip()
                    limit(compressedSize)
                }
                val restored = ByteBuffer.allocateDirect(payload.size)

                compressor.decompress(wire, restored) shouldBeEqualTo payload.size
                restored.flip()
                ByteArray(restored.remaining()).also(restored::get) shouldContentEqual payload
            }
            .run()
    }

    @ParameterizedTest(name = "{0} retry after overflow")
    @MethodSource("sharedCompressors")
    fun `overflow rolls target position back and permits retry`(
        name: String,
        compressor: Compressor,
    ) {
        val payload = payloadFor(name)
        val wire = compressor.compress(payload)
        val source = ByteBuffer.wrap(wire)
        val tooSmall = ByteBuffer.allocateDirect(payload.size - 1)

        assertFailsWith<BufferOverflowException> { compressor.decompress(source, tooSmall) }
        source.position() shouldBeEqualTo 0
        tooSmall.position() shouldBeEqualTo 0

        val retry = ByteBuffer.allocateDirect(payload.size)
        compressor.decompress(source, retry) shouldBeEqualTo payload.size
        retry.flip()
        ByteArray(retry.remaining()).also(retry::get) shouldContentEqual payload
    }

    private fun payloadFor(name: String): ByteArray =
        ByteArray(64 * 1024) { index -> ((index * 31 + name.length) and 0xFF).toByte() }

    companion object {
        @JvmStatic
        fun sharedCompressors(): Stream<Arguments> = Stream.of(
            Arguments.of("lz4", Compressors.LZ4),
            Arguments.of("deflate", Compressors.Deflate),
            Arguments.of("snappy", Compressors.Snappy),
            Arguments.of("zstd", Compressors.Zstd),
        )
    }
}
