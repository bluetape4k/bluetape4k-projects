package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State
import java.util.zip.ZipException
import kotlin.random.Random

class GzipCodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `GzipCodec rejects decompressed output larger than configured limit`() {
        val compressed = Compressors.GZip.compress(Random.nextBytes(128))
        val buf = Unpooled.wrappedBuffer(compressed)

        assertFailsWith<IllegalArgumentException> {
            GzipCodec(RedissonCodecs.String, maxDecompressedSize = 64)
                .valueDecoder
                .decode(buf, State())
        }
        buf.release()
    }

    @Test
    fun `GzipCodec propagates corrupt gzip payload failures`() {
        val buf = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3, 4))

        assertFailsWith<ZipException> {
            GzipCodec(RedissonCodecs.String).valueDecoder.decode(buf, State())
        }
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `GzipCodec roundtrips values within configured decompression limit`() {
        val codec = GzipCodec(RedissonCodecs.String, maxDecompressedSize = 1024)
        val original = faker.lorem().sentence()
        val buf = codec.valueEncoder.encode(original)

        codec.valueDecoder.decode(buf, State()) shouldBeEqualTo original
        buf.release()
    }
}
