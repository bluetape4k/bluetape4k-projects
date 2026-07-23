package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.io.serializer.BinarySerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class LettuceBinaryCodecBufferContractTest {

    @Test
    fun `built in target encoding uses the stream path and commits only the exact wire`() {
        binaryTargets().forEach { (name, target) ->
            val serializer = RecordingBinarySerializer()
            val codec = LettuceBinaryCodec<String>(serializer)
            try {
                target.writeByte(PREFIX)
                target.markReaderIndex()
                target.markWriterIndex()
                val start = target.writerIndex()
                val referenceCount = target.refCnt()

                codec.encodeValue(VALUE, target)

                serializer.streamCalls shouldBeEqualTo 1
                serializer.arrayCalls shouldBeEqualTo 0
                target.writerIndex() shouldBeEqualTo start + WIRE.size
                target.readerIndex() shouldBeEqualTo 0
                target.refCnt() shouldBeEqualTo referenceCount
                target.bytes(0, target.writerIndex()).contentEquals(byteArrayOf(PREFIX.toByte()) + WIRE).shouldBeTrue()
                target.resetReaderIndex()
                target.resetWriterIndex()
                target.readerIndex() shouldBeEqualTo 0
                target.writerIndex() shouldBeEqualTo start
            } catch (failure: Throwable) {
                throw AssertionError("binary target fixture failed: $name", failure)
            } finally {
                target.release()
            }
        }
    }

    @Test
    fun `one argument and caller owned target encodings keep identical wire`() {
        val serializer = RecordingBinarySerializer()
        val codec = LettuceBinaryCodec<String>(serializer)
        val target = Unpooled.buffer(1, 64)
        try {
            codec.encodeValue(VALUE, target)

            val oneArgument = codec.encodeValue(VALUE).remainingBytes()
            target.bytes(0, target.writerIndex()).contentEquals(oneArgument).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `null target returns before serializer dispatch`() {
        val serializer = RecordingBinarySerializer()

        LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, null)

        serializer.streamCalls shouldBeEqualTo 0
        serializer.arrayCalls shouldBeEqualTo 0
    }

    @Test
    fun `read only target fails before serializer dispatch and preserves observable state`() {
        val serializer = RecordingBinarySerializer()
        val target = Unpooled.buffer(8, 8).writeZero(7).readerIndex(3).asReadOnly()
        try {
            target.markReaderIndex()
            target.markWriterIndex()
            val referenceCount = target.refCnt()

            assertFailsWith<ReadOnlyBufferException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            }

            serializer.streamCalls shouldBeEqualTo 0
            serializer.arrayCalls shouldBeEqualTo 0
            target.readerIndex() shouldBeEqualTo 3
            target.writerIndex() shouldBeEqualTo 7
            target.refCnt() shouldBeEqualTo referenceCount
            target.resetReaderIndex()
            target.resetWriterIndex()
            target.readerIndex() shouldBeEqualTo 3
            target.writerIndex() shouldBeEqualTo 7
        } finally {
            target.release()
        }
    }

    @Test
    fun `reported and actual count mismatch fails without committing`() {
        val serializer = RecordingBinarySerializer().apply {
            streamBehavior = { output ->
                output.write(WIRE)
                WIRE.size + 1
            }
        }
        val target = Unpooled.buffer(1, 64)
        try {
            target.writeByte(PREFIX)
            val start = target.writerIndex()

            val failure = assertFailsWith<IllegalStateException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            }

            failure.message shouldBeEqualTo "Serializer reported 5 bytes but wrote 4 bytes."
            target.writerIndex() shouldBeEqualTo start
        } finally {
            target.release()
        }
    }

    @Test
    fun `writer and reader drift fail closed without recovery`() {
        listOf<(ByteBuf) -> Unit>(
            { target -> target.writerIndex(target.writerIndex() + 1) },
            { target -> target.readerIndex(target.readerIndex() + 1) },
        ).forEach { drift ->
            val target = Unpooled.buffer(8, 64)
            try {
                target.writeBytes(byteArrayOf(PREFIX.toByte(), 0x22))
                val serializer = RecordingBinarySerializer().apply {
                    streamBehavior = { output ->
                        output.write(WIRE)
                        drift(target)
                        WIRE.size
                    }
                }

                val failure = assertFailsWith<IllegalStateException> {
                    LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
                }

                failure.message shouldBeEqualTo "Target ByteBuf state changed during serialization."
            } finally {
                target.release()
            }
        }
    }

    @Test
    fun `reference count drift is not compensated by the codec`() {
        val target = BinaryLifecycleTrackingByteBuf(Unpooled.buffer(8, 64))
        try {
            val serializer = RecordingBinarySerializer().apply {
                streamBehavior = { output ->
                    output.write(WIRE)
                    target.retain()
                    target.resetCounts()
                    WIRE.size
                }
            }

            assertFailsWith<IllegalStateException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            }

            target.retainCalls shouldBeEqualTo 0
            target.releaseCalls shouldBeEqualTo 0
            target.refCnt() shouldBeEqualTo 2
        } finally {
            target.releaseDelegate(target.refCnt())
        }
    }

    @Test
    fun `serializer failure wins when failure and state drift happen together`() {
        val sentinel = IOException("serializer failure")
        val target = Unpooled.buffer(8, 64)
        try {
            target.writeByte(PREFIX)
            val serializer = RecordingBinarySerializer().apply {
                streamBehavior = { output ->
                    output.write(WIRE)
                    target.writerIndex(target.writerIndex() + 1)
                    throw sentinel
                }
            }

            val actual = assertFailsWith<IOException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            }

            actual shouldBeSameInstanceAs sentinel
        } finally {
            target.release()
        }
    }

    @Test
    fun `failed write stays uncommitted and the retained adapter is sealed`() {
        val sentinel = IOException("partial serializer failure")
        val serializer = RecordingBinarySerializer().apply {
            streamBehavior = { output ->
                retainedOutput = output
                output.write(WIRE)
                throw sentinel
            }
        }
        val target = Unpooled.buffer(8, 64)
        try {
            target.writeByte(PREFIX)
            val start = target.writerIndex()

            assertFailsWith<IOException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            } shouldBeSameInstanceAs sentinel

            target.writerIndex() shouldBeEqualTo start
            val sealedFailure = assertFailsWith<IOException> { requireNotNull(serializer.retainedOutput).write(0x33) }
            sealedFailure.message shouldBeEqualTo "Bounded ByteBuf output stream is sealed."
            target.writerIndex() shouldBeEqualTo start
        } finally {
            target.release()
        }
    }

    @Test
    fun `short retry does not commit dirty suffix from the previous failure`() {
        val sentinel = IOException("first attempt")
        var first = true
        val serializer = RecordingBinarySerializer().apply {
            streamBehavior = { output ->
                if (first) {
                    first = false
                    output.write(WIRE)
                    throw sentinel
                }
                output.write(byteArrayOf(0x09))
                1
            }
        }
        val target = Unpooled.buffer(2, 64)
        try {
            target.writeByte(PREFIX)
            val start = target.writerIndex()

            assertFailsWith<IOException> {
                LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)
            }
            LettuceBinaryCodec<String>(serializer).encodeValue(VALUE, target)

            target.writerIndex() shouldBeEqualTo start + 1
            target.bytes(0, target.writerIndex()).contentEquals(byteArrayOf(PREFIX.toByte(), 0x09)).shouldBeTrue()
            target.getUnsignedByte(start + 1).toInt() shouldBeEqualTo WIRE[1].toInt()
        } finally {
            target.release()
        }
    }

    @Test
    fun `target exhaustion and partial bulk failure never commit writer index`() {
        val bounded = Unpooled.buffer(2, 2)
        try {
            bounded.writeByte(PREFIX)
            val start = bounded.writerIndex()
            assertFailsWith<IllegalStateException> {
                LettuceBinaryCodec<String>(RecordingBinarySerializer()).encodeValue(VALUE, bounded)
            }
            bounded.writerIndex() shouldBeEqualTo start
        } finally {
            bounded.release()
        }

        val sentinel = IOException("partial target failure")
        val partial = BinaryPartialFailingByteBuf(Unpooled.buffer(8, 64), sentinel)
        try {
            partial.writeByte(PREFIX)
            val start = partial.writerIndex()
            val actual = assertFailsWith<IOException> {
                LettuceBinaryCodec<String>(RecordingBinarySerializer()).encodeValue(VALUE, partial)
            }
            actual shouldBeSameInstanceAs sentinel
            partial.writerIndex() shouldBeEqualTo start
        } finally {
            partial.release()
        }
    }

    @Test
    fun `raw Fory codecs preserve caller owned pooled targets across successful writes`() {
        rawForyCodecFactories().forEach { (codecName, codecFactory) ->
            val codec = codecFactory()
            val expected = codec.encodeValue(RAW_FORY_VALUE).remainingBytes()

            pooledRawTargets(initialCapacity = 8, maxCapacity = 512).forEach { (targetName, targetFactory) ->
                val target = targetFactory()
                try {
                    val start = prepareRawForyTarget(target, suffixIndex = RAW_START + expected.size)
                    val readerMark = target.readerIndex()
                    val writerMark = target.writerIndex()
                    val referenceCount = target.refCnt()

                    codec.encodeValue(RAW_FORY_VALUE, target)

                    target.readerIndex() shouldBeEqualTo readerMark
                    target.writerIndex() shouldBeEqualTo start + expected.size
                    target.refCnt() shouldBeEqualTo referenceCount
                    target.getUnsignedByte(RAW_PREFIX_INDEX).toInt() shouldBeEqualTo RAW_PREFIX
                    target.getUnsignedByte(RAW_SUFFIX_INDEX).toInt() shouldBeEqualTo RAW_SUFFIX
                    target.getUnsignedByte(start + expected.size).toInt() shouldBeEqualTo RAW_SUFFIX
                    target.bytes(start, expected.size).contentEquals(expected).shouldBeTrue()
                    codec.decodeValue(ByteBuffer.wrap(target.bytes(start, expected.size))) shouldBeEqualTo RAW_FORY_VALUE
                    assertRawForyMarks(target, readerMark, writerMark)
                } catch (failure: Throwable) {
                    throw AssertionError("raw Fory success fixture failed: $codecName/$targetName", failure)
                } finally {
                    target.release()
                }
            }
        }
    }

    @Test
    fun `raw Fory codecs leave pooled targets uncommitted when bounded capacity is insufficient`() {
        rawForyCodecFactories().forEach { (codecName, codecFactory) ->
            val codec = codecFactory()
            val expected = codec.encodeValue(RAW_FORY_VALUE).remainingBytes()
            val maxCapacity = RAW_START + expected.size - 1

            pooledRawTargets(initialCapacity = maxCapacity, maxCapacity = maxCapacity).forEach { (targetName, targetFactory) ->
                val target = targetFactory()
                try {
                    val start = prepareRawForyTarget(target, suffixIndex = maxCapacity - 1)
                    val readerMark = target.readerIndex()
                    val writerMark = target.writerIndex()
                    val referenceCount = target.refCnt()

                    assertFailsWith<Throwable> {
                        codec.encodeValue(RAW_FORY_VALUE, target)
                    }

                    assertRawForyFailureState(target, start, readerMark, writerMark, referenceCount, maxCapacity - 1)
                } catch (failure: Throwable) {
                    throw AssertionError("raw Fory capacity fixture failed: $codecName/$targetName", failure)
                } finally {
                    target.release()
                }
            }
        }
    }

    @Test
    fun `raw Fory codecs leave pooled targets uncommitted when destination writes fail`() {
        rawForyCodecFactories().forEach { (codecName, codecFactory) ->
            val codec = codecFactory()
            val expected = codec.encodeValue(RAW_FORY_VALUE).remainingBytes()
            val maxCapacity = RAW_START + expected.size + 8

            pooledRawTargets(initialCapacity = maxCapacity, maxCapacity = maxCapacity).forEach { (targetName, targetFactory) ->
                val delegate = targetFactory()
                val destinationFailure = IOException("raw Fory destination failure")
                val target = BinaryPartialFailingByteBuf(delegate, destinationFailure)
                try {
                    val start = prepareRawForyTarget(target, suffixIndex = RAW_START + expected.size + 1)
                    val readerMark = target.readerIndex()
                    val writerMark = target.writerIndex()
                    val referenceCount = target.refCnt()

                    assertFailsWith<IOException> {
                        codec.encodeValue(RAW_FORY_VALUE, target)
                    } shouldBeSameInstanceAs destinationFailure

                    assertRawForyFailureState(target, start, readerMark, writerMark, referenceCount, RAW_START + expected.size + 1)
                } catch (failure: Throwable) {
                    throw AssertionError("raw Fory destination fixture failed: $codecName/$targetName", failure)
                } finally {
                    target.release()
                }
            }
        }
    }

    @Test
    fun `failure diagnostics never expose the serialized value`() {
        val secret = "issue756-secret-value"
        val target = Unpooled.buffer(0, 0)
        try {
            val failure = assertFailsWith<IllegalStateException> {
                LettuceBinaryCodec<String>(RecordingBinarySerializer()).encodeValue(secret, target)
            }

            (failure.message?.contains(secret) == false).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `decode borrows only a bounded read only remaining view and preserves caller state`() {
        binarySources().forEach { (name, source) ->
            val retainedViews = mutableListOf<ByteBuffer>()
            val serializer = RecordingBinarySerializer().apply {
                deserializeBehavior = { view ->
                    retainedViews += view
                    view.position() shouldBeEqualTo 0
                    view.limit() shouldBeEqualTo WIRE.size
                    view.capacity() shouldBeEqualTo WIRE.size
                    view.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
                    view.isReadOnly.shouldBeTrue()
                    (!view.hasArray()).shouldBeTrue()
                    assertFailsWith<UnsupportedOperationException> { view.array() }
                    assertFailsWith<UnsupportedOperationException> { view.arrayOffset() }
                    assertFailsWith<ReadOnlyBufferException> { view.put(0, 0x33.toByte()) }
                    view.clear()
                    view.remainingBytes().contentEquals(WIRE).shouldBeTrue()
                    VALUE
                }
            }
            val start = source.position()
            val limit = source.limit()
            val order = source.order()

            try {
                LettuceBinaryCodec<String>(serializer).decodeValue(source) shouldBeEqualTo VALUE

                serializer.deserializeBufferCalls shouldBeEqualTo 1
                serializer.deserializeArrayCalls shouldBeEqualTo 0
                source.position() shouldBeEqualTo start
                source.limit() shouldBeEqualTo limit
                source.order() shouldBeEqualTo order
                source.reset()
                source.position() shouldBeEqualTo start
                retainedViews.single().capacity() shouldBeEqualTo WIRE.size
                assertFailsWith<IndexOutOfBoundsException> { retainedViews.single().get(WIRE.size) }
            } catch (failure: Throwable) {
                throw AssertionError("binary decode source fixture failed: $name", failure)
            }
        }
    }

    @Test
    fun `decode failure keeps caller state and retained view cannot expose surrounding secrets`() {
        val sentinel = IOException("bounded decode failure")
        lateinit var retainedView: ByteBuffer
        val serializer = RecordingBinarySerializer().apply {
            deserializeBehavior = { view ->
                retainedView = view
                view.clear()
                throw sentinel
            }
        }
        val source = binarySources().first().second
        val start = source.position()
        val limit = source.limit()
        val order = source.order()

        val actual = assertFailsWith<IOException> {
            LettuceBinaryCodec<String>(serializer).decodeValue(source)
        }

        actual shouldBeSameInstanceAs sentinel
        source.position() shouldBeEqualTo start
        source.limit() shouldBeEqualTo limit
        source.order() shouldBeEqualTo order
        source.reset()
        source.position() shouldBeEqualTo start
        retainedView.capacity() shouldBeEqualTo WIRE.size
        retainedView.remainingBytes().contentEquals(WIRE).shouldBeTrue()
        assertFailsWith<IndexOutOfBoundsException> { retainedView.get(WIRE.size) }
        (actual.message?.contains(PREFIX_SECRET) == false).shouldBeTrue()
        (actual.message?.contains(SUFFIX_SECRET) == false).shouldBeTrue()
    }

    @Test
    fun `decode preserves the interface allocating fallback for serializers without a direct override`() {
        val serializer = object: BinarySerializer {
            var arrayCalls: Int = 0

            override fun serialize(graph: Any?): ByteArray = WIRE.copyOf()

            @Suppress("UNCHECKED_CAST")
            override fun <T: Any> deserialize(bytes: ByteArray?): T? {
                arrayCalls++
                bytes?.contentEquals(WIRE).shouldBeTrue()
                return VALUE as T
            }
        }
        val source = binarySources().first().second

        LettuceBinaryCodec<String>(serializer).decodeValue(source) shouldBeEqualTo VALUE

        serializer.arrayCalls shouldBeEqualTo 1
    }

    private class RecordingBinarySerializer: BinarySerializer {
        var arrayCalls: Int = 0
            private set
        var streamCalls: Int = 0
            private set
        var deserializeArrayCalls: Int = 0
            private set
        var deserializeBufferCalls: Int = 0
            private set
        var retainedOutput: OutputStream? = null
        var streamBehavior: (OutputStream) -> Int = { output ->
            output.write(WIRE)
            WIRE.size
        }

        override fun serialize(graph: Any?): ByteArray = WIRE.copyOf().also { arrayCalls++ }

        override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
            streamCalls++
            return streamBehavior(target)
        }

        var deserializeBehavior: (ByteBuffer) -> Any? = { VALUE }

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> deserialize(bytes: ByteArray?): T? {
            deserializeArrayCalls++
            return VALUE as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
            deserializeBufferCalls++
            return deserializeBehavior(source) as T?
        }
    }

    @Suppress("DEPRECATION")
    private class BinaryPartialFailingByteBuf(
        delegate: ByteBuf,
        private val failure: IOException,
    ): io.netty.buffer.DuplicatedByteBuf(delegate) {
        override fun setBytes(index: Int, source: ByteArray, sourceIndex: Int, length: Int): ByteBuf {
            super.setBytes(index, source, sourceIndex, minOf(2, length))
            throw failure
        }
    }

    @Suppress("DEPRECATION")
    private class BinaryLifecycleTrackingByteBuf(
        private val delegate: ByteBuf,
    ): io.netty.buffer.SwappedByteBuf(delegate) {
        var retainCalls: Int = 0
            private set
        var releaseCalls: Int = 0
            private set

        override fun retain(): ByteBuf {
            retainCalls++
            return super.retain()
        }

        override fun retain(increment: Int): ByteBuf {
            retainCalls++
            return super.retain(increment)
        }

        override fun release(): Boolean {
            releaseCalls++
            return super.release()
        }

        override fun release(decrement: Int): Boolean {
            releaseCalls++
            return super.release(decrement)
        }

        fun resetCounts() {
            retainCalls = 0
            releaseCalls = 0
        }

        fun releaseDelegate(decrement: Int) {
            delegate.release(decrement)
        }
    }

    private fun binaryTargets(): List<Pair<String, ByteBuf>> = listOf(
        "unpooled heap" to Unpooled.buffer(4, 64),
        "unpooled direct" to Unpooled.directBuffer(4, 64),
        "pooled heap" to PooledByteBufAllocator.DEFAULT.heapBuffer(4, 64),
        "pooled direct" to PooledByteBufAllocator.DEFAULT.directBuffer(4, 64),
        "bounded slice" to Unpooled.buffer(64, 64).slice(0, 16).clear(),
    )

    private fun rawForyCodecFactories(): List<Pair<String, () -> LettuceBinaryCodec<String>>> = listOf(
        "Fory" to { LettuceBinaryCodecs.fory<String>() },
        "FastFory" to { LettuceBinaryCodecs.fastFory<String>() },
    )

    private fun pooledRawTargets(initialCapacity: Int, maxCapacity: Int): List<Pair<String, () -> ByteBuf>> = listOf(
        "pooled heap" to { PooledByteBufAllocator.DEFAULT.heapBuffer(initialCapacity, maxCapacity) },
        "pooled direct" to { PooledByteBufAllocator.DEFAULT.directBuffer(initialCapacity, maxCapacity) },
    )

    private fun prepareRawForyTarget(target: ByteBuf, suffixIndex: Int): Int {
        target.capacity(suffixIndex + 1)
        target.setByte(RAW_PREFIX_INDEX, RAW_PREFIX)
        target.setByte(RAW_SUFFIX_INDEX, RAW_SUFFIX)
        target.writerIndex(RAW_START)
        target.readerIndex(RAW_READER_INDEX)
        target.markReaderIndex()
        target.markWriterIndex()
        target.setByte(suffixIndex, RAW_SUFFIX)
        return target.writerIndex()
    }

    private fun assertRawForyFailureState(
        target: ByteBuf,
        start: Int,
        readerMark: Int,
        writerMark: Int,
        referenceCount: Int,
        suffixIndex: Int,
    ) {
        target.readerIndex() shouldBeEqualTo readerMark
        target.writerIndex() shouldBeEqualTo start
        target.refCnt() shouldBeEqualTo referenceCount
        target.getUnsignedByte(RAW_PREFIX_INDEX).toInt() shouldBeEqualTo RAW_PREFIX
        target.getUnsignedByte(RAW_SUFFIX_INDEX).toInt() shouldBeEqualTo RAW_SUFFIX
        target.getUnsignedByte(suffixIndex).toInt() shouldBeEqualTo RAW_SUFFIX
        assertRawForyMarks(target, readerMark, writerMark)
    }

    private fun assertRawForyMarks(target: ByteBuf, readerMark: Int, writerMark: Int) {
        target.resetReaderIndex()
        target.readerIndex() shouldBeEqualTo readerMark
        target.resetWriterIndex()
        target.writerIndex() shouldBeEqualTo writerMark
    }

    private fun binarySources(): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to configuredBinarySource(ByteBuffer.allocate(WIRE.size + 4)),
        "direct" to configuredBinarySource(ByteBuffer.allocateDirect(WIRE.size + 4)),
        "slice" to configuredBinarySource(ByteBuffer.allocate(WIRE.size + 8).position(2).slice()),
        "read only" to configuredBinarySource(ByteBuffer.allocate(WIRE.size + 4)).asReadOnlyBuffer().apply {
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        },
    )

    private fun configuredBinarySource(source: ByteBuffer): ByteBuffer = source.apply {
        put(PREFIX_SECRET.encodeToByteArray())
        put(WIRE)
        put(SUFFIX_SECRET.encodeToByteArray())
        position(PREFIX_SECRET.length)
        limit(PREFIX_SECRET.length + WIRE.size)
        order(ByteOrder.LITTLE_ENDIAN)
        mark()
    }

    private fun ByteBuf.bytes(index: Int, length: Int): ByteArray =
        ByteArray(length).also { bytes -> getBytes(index, bytes) }

    private fun ByteBuffer.remainingBytes(): ByteArray =
        ByteArray(remaining()).also { bytes -> duplicate().get(bytes) }

    private companion object {
        const val PREFIX: Int = 0x5A
        const val PREFIX_SECRET: String = "P!"
        const val SUFFIX_SECRET: String = "S!"
        const val VALUE: String = "buffer-contract"
        val WIRE: ByteArray = byteArrayOf(1, 2, 3, 4)
        const val RAW_FORY_VALUE: String = "raw-fory-buffer-contract"
        const val RAW_PREFIX_INDEX: Int = 0
        const val RAW_READER_INDEX: Int = 1
        const val RAW_SUFFIX_INDEX: Int = 1
        const val RAW_START: Int = 2
        const val RAW_PREFIX: Int = 0x5A
        const val RAW_SUFFIX: Int = 0x6B
    }
}
