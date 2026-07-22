package io.bluetape4k.redis.lettuce.codec

import io.netty.buffer.ByteBuf
import java.io.IOException
import java.io.OutputStream
import java.util.Objects

internal class BoundedByteBufOutputStream(
    private val target: ByteBuf,
): OutputStream() {
    private val start = target.writerIndex()
    private val readerIndex = target.readerIndex()
    private val referenceCount = target.refCnt()
    private val maxCapacity = target.maxCapacity()
    private var written = 0
    private var highWater = 0
    private var sealed = false

    fun writtenBytes(): Int = written

    fun highWaterBytes(): Int = highWater

    fun startIndex(): Int = start

    fun seal() {
        sealed = true
    }

    fun verifySnapshot() {
        checkSnapshot()
    }

    override fun write(value: Int) {
        checkOpen()
        val nextWritten = Math.addExact(written, 1)
        prepareWrite(nextWritten)
        highWater = maxOf(highWater, nextWritten)
        target.setByte(start + written, value)
        written = nextWritten
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        checkOpen()
        Objects.checkFromIndexSize(offset, length, bytes.size)
        val nextWritten = Math.addExact(written, length)
        prepareWrite(nextWritten)
        highWater = maxOf(highWater, nextWritten)
        target.setBytes(start + written, bytes, offset, length)
        written = nextWritten
    }

    override fun flush() = Unit

    override fun close() = Unit

    private fun prepareWrite(nextWritten: Int) {
        val requiredEnd = Math.addExact(start, nextWritten)
        check(requiredEnd <= maxCapacity) {
            "Serialized output exceeds target maxCapacity."
        }
        checkSnapshot()
        target.ensureWritable(nextWritten)
        checkSnapshot()
    }

    private fun checkOpen() {
        if (sealed) {
            throw IOException("Bounded ByteBuf output stream is sealed.")
        }
    }

    private fun checkSnapshot() {
        check(
            target.writerIndex() == start &&
                    target.readerIndex() == readerIndex &&
                    target.refCnt() == referenceCount &&
                    target.maxCapacity() == maxCapacity,
        ) {
            "Target ByteBuf state changed during serialization."
        }
    }
}
