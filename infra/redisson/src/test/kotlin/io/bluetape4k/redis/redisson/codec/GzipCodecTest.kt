package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.io.compressor.Compressors
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State
import java.util.zip.ZipException

class GzipCodecTest {

    @Test
    fun `GzipCodec rejects decompressed output larger than configured limit`() {
        val compressed = Compressors.GZip.compress(ByteArray(128) { 'A'.code.toByte() })
        val buf = Unpooled.wrappedBuffer(compressed)

        try {
            assertFailsWith<IllegalArgumentException> {
                GzipCodec(RedissonCodecs.String, maxDecompressedSize = 64)
                    .valueDecoder
                    .decode(buf, State())
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `GzipCodec propagates corrupt gzip payload failures`() {
        val buf = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3, 4))

        try {
            assertFailsWith<ZipException> {
                GzipCodec(RedissonCodecs.String).valueDecoder.decode(buf, State())
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `GzipCodec roundtrips values within configured decompression limit`() {
        val codec = GzipCodec(RedissonCodecs.String, maxDecompressedSize = 1024)
        val buf = codec.valueEncoder.encode("bounded gzip")

        try {
            codec.valueDecoder.decode(buf, State()) shouldBeEqualTo "bounded gzip"
        } finally {
            buf.release()
        }
    }
}
