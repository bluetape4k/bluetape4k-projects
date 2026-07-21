package io.bluetape4k.io.compressor

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.nio.ByteOrder

class CompressorBufferSupportTest {

    @Test
    fun `operation views isolate caller cursor limit order and mark`() {
        val source = CompressorByteBufferTestSupport.heap(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(8, direct = true)
        val sourceStart = source.position()
        val sourceLimit = source.limit()
        val targetStart = target.position()
        val targetLimit = target.limit()

        val written = writeToCallerBufferViews(source, target) { sourceView, targetView, _, _, _, _ ->
            sourceView.clear()
            sourceView.order(ByteOrder.BIG_ENDIAN)
            targetView.clear()
            targetView.order(ByteOrder.BIG_ENDIAN)
            0
        }

        written shouldBeEqualTo 0
        source.position() shouldBeEqualTo sourceStart
        source.limit() shouldBeEqualTo sourceLimit
        source.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        CompressorByteBufferTestSupport.assertMark(source, sourceStart)
        target.position() shouldBeEqualTo targetStart
        target.limit() shouldBeEqualTo targetLimit
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        CompressorByteBufferTestSupport.assertMark(target, targetStart)
    }

    @Test
    fun `view mutation cannot mask a pre-created fatal failure`() {
        val expected = AssertionError("fatal")
        val source = CompressorByteBufferTestSupport.heap(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(8, direct = false)
        val sourceStart = source.position()
        val targetStart = target.position()

        val actual = assertFailsWith<Throwable> {
            writeToCallerBufferViews(source, target) { sourceView, targetView, _, _, _, _ ->
                sourceView.clear()
                targetView.limit(0)
                throw expected
            }
        }

        assertSame(expected, actual)
        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
        CompressorByteBufferTestSupport.assertMark(source, sourceStart)
        CompressorByteBufferTestSupport.assertMark(target, targetStart)
    }

    @Test
    fun `invalid written count fails without moving caller buffers`() {
        val source = CompressorByteBufferTestSupport.heap(byteArrayOf(1, 2, 3))
        val target = CompressorByteBufferTestSupport.writableTarget(8, direct = false)
        val sourceStart = source.position()
        val targetStart = target.position()

        assertFailsWith<IllegalStateException> {
            writeToCallerBufferViews(source, target) { _, _, _, _, _, targetRemaining -> targetRemaining + 1 }
        }

        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
        CompressorByteBufferTestSupport.assertMark(source, sourceStart)
        CompressorByteBufferTestSupport.assertMark(target, targetStart)
    }
}
