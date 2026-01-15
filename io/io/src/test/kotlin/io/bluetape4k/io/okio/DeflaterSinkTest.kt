package io.bluetape4k.io.okio

import io.bluetape4k.io.okio.TestUtil.SEGMENT_SIZE
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okio.Buffer
import okio.Deflater
import okio.DeflaterSink
import okio.IOException
import okio.Inflater
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.zip.InflaterInputStream
import kotlin.random.Random

class DeflaterSinkTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `deflate with close`() {
        val original = Fakers.randomString(SEGMENT_SIZE)
        val data = bufferOf(original)

        val sink = Buffer()

        DeflaterSink(sink, Deflater()).use { deflaterSink ->
            deflaterSink.write(data, data.size)  // data 를 읽어서 deflaterSink에 쓴다
        }

        inflate(sink).use { inflated ->
            inflated.readUtf8() shouldBeEqualTo original
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `deflate with sync flush`() {
        val original = Fakers.randomString(SEGMENT_SIZE)
        val data = bufferOf(original)

        val sink = Buffer()

        DeflaterSink(sink, Deflater()).use { deflaterSink ->
            deflaterSink.write(data, data.size)
            deflaterSink.flush()
        }

        inflate(sink).use { inflated ->
            inflated.readUtf8() shouldBeEqualTo original
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `deflate well compressed`() {
        val original = "a".repeat(1024 * 1024)
        val data = bufferOf(original)

        val sink = Buffer()

        DeflaterSink(sink, Deflater()).use { deflaterSink ->
            deflaterSink.write(data, data.size)
        }

        inflate(sink).use { inflated ->
            inflated.readUtf8() shouldBeEqualTo original
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `deflate poorly compressed`() {
        val original = byteStringOf(Random.nextBytes(1024 * 1024))
        val data = bufferOf(original)

        val sink = Buffer()

        DeflaterSink(sink, Deflater()).use {
            it.write(data, data.size)
        }

        inflate(sink).use {
            it.readByteString() shouldBeEqualTo original
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `multiple segments without compression`() {
        val buffer = Buffer()
        val deflater = Deflater().apply { setLevel(Deflater.NO_COMPRESSION) }
        val byteCount = SEGMENT_SIZE * 4

        DeflaterSink(buffer, deflater).use {
            it.write(bufferOf("a".repeat(byteCount)), byteCount.toLong())
        }

        inflate(buffer).use {
            it.readUtf8(byteCount.toLong()) shouldBeEqualTo "a".repeat(byteCount)
        }
    }

    @Test
    fun `deflate into non empty sink`() {
        val original = Fakers.randomString(SEGMENT_SIZE)

        repeat(SEGMENT_SIZE) {
            log.debug { "Deflater 압축[$it]" }
            val data = bufferOf(original)
            val sink = Buffer().writeUtf8("a".repeat(it))

            DeflaterSink(sink, Deflater()).use { deflaterSink ->
                deflaterSink.write(data, data.size)
            }

            // 기존 정보를 건너 뛴다 
            sink.skip(it.toLong())

            inflate(sink).use { inflated ->
                inflated.readUtf8() shouldBeEqualTo original
            }
        }
    }

    /**
     * This test deflates a single segment of without compression because that's
     * the easiest way to force close() to emit a large amount of data to the
     * underlying sink.
     */
    @RepeatedTest(REPEAT_SIZE)
    fun `close with exception when writing and closing`() {
        val mockSink = MockSink()
        mockSink.scheduleThrow(0, IOException("first"))
        mockSink.scheduleThrow(1, IOException("second"))

        val deflater = Deflater().apply { setLevel(Deflater.NO_COMPRESSION) }
        val deflaterSink = DeflaterSink(mockSink, deflater)
        deflaterSink.write(bufferOf("a".repeat(SEGMENT_SIZE)), SEGMENT_SIZE.toLong())

        assertFailsWith<IOException> {
            deflaterSink.close()
        }.message shouldBeEqualTo "first"

        mockSink.assertLogContains("close()")
    }

    /**
     * 이 테스트는 Deflater에서 NullPointerException을 IOException으로 다시 던지는지 확인합니다.
     */
    @Test
    fun `rethrow null pointer as IOException`() {
        val deflater = Deflater()
        // close to cause a NullPointerException
        deflater.end()

        val data = bufferOf(Fakers.randomString())
        val deflaterSink = DeflaterSink(data, deflater)

        assertFailsWith<IOException> {
            deflaterSink.write(data, data.size)
        }.cause shouldBeInstanceOf NullPointerException::class
    }

    /**
     * Uses streaming decompression to inflate `deflated`.
     * The input must either be finish
     */
    private fun inflate(deflated: Buffer): Buffer {
        val deflatedIn = deflated.inputStream()
        val inflater = Inflater()
        val inflatedIn = InflaterInputStream(deflatedIn, inflater)

        val result = Buffer()
        val buffer = ByteArray(SEGMENT_SIZE)

        while (!inflater.needsInput() || deflated.size > 0 || inflatedIn.available() > 0) {
            // while (inflatedIn.available() > 0) {
            runCatching { inflatedIn.read(buffer, 0, buffer.size) }
                .onSuccess { count ->
                    if (count != -1) {
                        result.write(buffer, 0, count)
                    }
                }
                .onFailure {
                    return result
                }
        }
        return result
    }
}
