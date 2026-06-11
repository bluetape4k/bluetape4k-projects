package io.bluetape4k.io.compressor

import kotlin.math.max

internal class CompressorByteArrayBuffer(initialCapacity: Int) {

    private var buffer = ByteArray(max(initialCapacity, 32))
    private var count = 0

    fun write(value: Int) {
        ensureCapacity(count + 1)
        buffer[count++] = value.toByte()
    }

    fun write(source: ByteArray, offset: Int = 0, length: Int = source.size) {
        if (length == 0) {
            return
        }

        ensureCapacity(count + length)
        source.copyInto(buffer, destinationOffset = count, startIndex = offset, endIndex = offset + length)
        count += length
    }

    fun writeIntLe(value: Int) {
        write(value)
        write(value ushr 8)
        write(value ushr 16)
        write(value ushr 24)
    }

    fun toByteArray(): ByteArray = buffer.copyOf(count)

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity <= buffer.size) {
            return
        }

        var newCapacity = buffer.size shl 1
        while (newCapacity < minCapacity) {
            newCapacity = newCapacity shl 1
        }
        buffer = buffer.copyOf(newCapacity)
    }
}
