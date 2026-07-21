package io.bluetape4k.io.compressor

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

internal object CompressorByteBufferTestSupport {
    const val FILL: Byte = 0x5A

    val payload: ByteArray = "caller-owned compressor payload".repeat(32).encodeToByteArray()

    fun heap(bytes: ByteArray, prefix: Int = 7, suffix: Int = 11): ByteBuffer =
        source(bytes, prefix, suffix, direct = false)

    fun direct(bytes: ByteArray, prefix: Int = 7, suffix: Int = 11): ByteBuffer =
        source(bytes, prefix, suffix, direct = true)

    fun heapSlice(bytes: ByteArray): ByteBuffer = sliceSource(bytes, direct = false)

    fun directSlice(bytes: ByteArray): ByteBuffer = sliceSource(bytes, direct = true)

    fun sources(bytes: ByteArray): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to heap(bytes),
        "direct" to direct(bytes),
        "heap-slice" to heapSlice(bytes),
        "direct-slice" to directSlice(bytes),
        "heap-read-only" to heap(bytes).asReadOnlyBuffer().apply { order(ByteOrder.LITTLE_ENDIAN) },
        "direct-read-only" to direct(bytes).asReadOnlyBuffer().apply { order(ByteOrder.LITTLE_ENDIAN) },
    )

    fun writableTarget(capacity: Int, direct: Boolean, prefix: Int = 5): ByteBuffer =
        newBuffer(prefix + capacity + 13, direct).apply {
            repeat(capacity()) { put(FILL) }
            clear()
            order(ByteOrder.LITTLE_ENDIAN)
            position(prefix)
            limit(prefix + capacity)
            mark()
        }

    fun slicedTarget(capacity: Int, direct: Boolean): ByteBuffer {
        val parent = newBuffer(capacity + 23, direct)
        repeat(parent.capacity()) { parent.put(FILL) }
        parent.position(3)
        parent.limit(parent.capacity() - 4)
        return parent.slice().apply {
            order(ByteOrder.LITTLE_ENDIAN)
            position(5)
            limit(5 + capacity)
            mark()
        }
    }

    fun targets(capacity: Int): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to writableTarget(capacity, direct = false),
        "direct" to writableTarget(capacity, direct = true),
        "heap-slice" to slicedTarget(capacity, direct = false),
        "direct-slice" to slicedTarget(capacity, direct = true),
    )

    fun bytes(buffer: ByteBuffer, start: Int, size: Int): ByteArray =
        ByteArray(size).also { bytes ->
            buffer.duplicate().position(start).limit(start + size).get(bytes)
        }

    fun allBytes(buffer: ByteBuffer): ByteArray = bytes(buffer, 0, buffer.capacity())

    fun assertMark(buffer: ByteBuffer, expected: Int) {
        val duplicate = buffer.duplicate()
        duplicate.reset()
        check(duplicate.position() == expected) {
            "Expected mark=$expected, actual=${duplicate.position()}"
        }
    }

    private fun source(bytes: ByteArray, prefix: Int, suffix: Int, direct: Boolean): ByteBuffer =
        newBuffer(prefix + bytes.size + suffix, direct).apply {
            repeat(capacity()) { put(FILL) }
            clear()
            position(prefix)
            put(bytes)
            limit(position())
            position(prefix)
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        }

    private fun sliceSource(bytes: ByteArray, direct: Boolean): ByteBuffer {
        val parent = newBuffer(bytes.size + 31, direct)
        repeat(parent.capacity()) { parent.put(FILL) }
        parent.clear()
        parent.position(4)
        parent.limit(parent.capacity() - 6)
        return parent.slice().apply {
            position(7)
            put(bytes)
            limit(position())
            position(7)
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        }
    }

    private fun newBuffer(capacity: Int, direct: Boolean): ByteBuffer =
        if (direct) ByteBuffer.allocateDirect(capacity) else ByteBuffer.allocate(capacity)
}

internal open class ReversingFallbackCompressor: Compressor {
    val compressInvocations = AtomicInteger()
    val decompressInvocations = AtomicInteger()

    override fun compress(plain: ByteArray?): ByteArray {
        compressInvocations.incrementAndGet()
        return (plain ?: ByteArray(0)).reversedArray()
    }

    override fun decompress(compressed: ByteArray?): ByteArray {
        decompressInvocations.incrementAndGet()
        return (compressed ?: ByteArray(0)).reversedArray()
    }
}
