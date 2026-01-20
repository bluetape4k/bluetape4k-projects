package io.bluetape4k.netty.buffer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireInRange
import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCounted
import kotlin.math.ceil

/**
 * Bits 를 저장하는 버퍼를 나태내는 [BitBuf]의 구현체.
 */
class BitBufImpl internal constructor(override val byteBuf: ByteBuf): BitBuf {

    companion object: KLogging() {
        const val BITS_SIZE: Int = Byte.SIZE_BITS
        const val BITS_SIZE_DOUBLE: Double = BITS_SIZE.toDouble()
    }

    override val capacity: Long
        get() = byteBuf.capacity().toLong() * BITS_SIZE

    override val maxCapacity: Long
        get() = byteBuf.maxCapacity().toLong() * BITS_SIZE


    override var readerIndex: Long =
        byteBuf.readerIndex().toLong() * BITS_SIZE
        get() {
            if (byteBuf.readerIndex() != ceil(field / BITS_SIZE_DOUBLE).toInt()) {
                field =
                    byteBuf.readerIndex().toLong() * BITS_SIZE
            }
            return field
        }
        set(value) {
            field = value
            byteBuf.readerIndex(ceil(field / BITS_SIZE_DOUBLE).toInt())
        }

    override var writerIndex: Long = byteBuf.writerIndex().toLong() * Byte.SIZE_BITS
        get() {
            if (byteBuf.writerIndex() != ceil(field / BITS_SIZE_DOUBLE).toInt()) {
                field =
                    byteBuf.writerIndex().toLong() * BITS_SIZE
            }
            return field
        }
        set(value) {
            field = value
            byteBuf.writerIndex(ceil(field / BITS_SIZE_DOUBLE).toInt())
        }

    override fun getBoolean(index: Long): Boolean = getUnsignedBits(index, 1) == 1u

    override fun getUnsignedBits(index: Long, amount: Int): UInt {
        amount.requireInRange(1, Int.SIZE_BITS, "amount")
        if (index < 0 || (index + amount) > capacity) {
            throw IndexOutOfBoundsException("index:$index, length:$capacity (expected: range(0, $capacity)): $this")
        }

        var byteIndex = (index / BITS_SIZE).toInt()
        var relBitIndex = index.toInt() and (BITS_SIZE - 1)
        var value = 0u
        var remBits = amount
        while (remBits > 0) {
            val bitsToGet =
                minOf(BITS_SIZE - relBitIndex, remBits)
            val shift =
                (BITS_SIZE - (relBitIndex + bitsToGet)) and (BITS_SIZE - 1)
            val mask = (1u shl bitsToGet) - 1u
            value = (value shl bitsToGet) or ((byteBuf.getUnsignedByte(byteIndex).toUInt() shr shift) and mask)
            remBits -= bitsToGet
            relBitIndex = 0
            byteIndex++
        }
        return value
    }

    override fun setBits(index: Long, amount: Int, value: Int): BitBuf = apply {
        amount.requireInRange(1, Int.SIZE_BITS, "amount")
        if (index < 0 || (index + amount) > capacity) {
            throw IndexOutOfBoundsException("index:$index, length:$capacity (expected: range(0, $capacity)): $this")
        }
        var byteIndex = (index / BITS_SIZE).toInt()
        var relBitIndex = index.toInt() and (BITS_SIZE - 1)
        var remBits = amount
        while (remBits > 0) {
            val bitsToSet =
                minOf(BITS_SIZE - relBitIndex, remBits)
            val shift =
                (BITS_SIZE - (relBitIndex + bitsToSet)) and (BITS_SIZE - 1)
            val mask = (1 shl bitsToSet) - 1
            val iValue = (byteBuf.getUnsignedByte(byteIndex).toInt() and (mask shl shift).inv()) or
                    (((value shr (remBits - bitsToSet)) and mask) shl shift)
            byteBuf.setByte(byteIndex, iValue)
            remBits -= bitsToSet
            relBitIndex = 0
            byteIndex++
        }
    }

    override fun readBoolean(): Boolean = readUnsignedBits(1) == 1u

    override fun readUnsignedBits(amount: Int): UInt {
        if (readableBits() < amount) {
            throw IndexOutOfBoundsException(
                "readerIndex($readerIndex) + length($amount) exceeds writerIndex($writerIndex): $this"
            )
        }
        val value = getUnsignedBits(readerIndex, amount)
        readerIndex += amount
        return value
    }

    override fun writeBoolean(value: Boolean): BitBuf =
        writeBits(if (value) 1 else 0, 1)

    override fun writeBits(value: Int, amount: Int): BitBuf = apply {
        if (writableBits() < amount) {
            throw IndexOutOfBoundsException(
                "writerIndex($writerIndex) + minWritableBits($amount) exceeds maxCapacity($maxCapacity): $this"
            )
        }
        setBits(writerIndex, amount, value)
        writerIndex += amount
    }

    override fun refCnt(): Int = byteBuf.refCnt()

    override fun retain(): ReferenceCounted = byteBuf.retain()

    override fun retain(increment: Int): ReferenceCounted = byteBuf.retain(increment)

    override fun touch(): ReferenceCounted = byteBuf.touch()

    override fun touch(hint: Any?): ReferenceCounted = byteBuf.touch(hint)

    override fun release(): Boolean = byteBuf.release()

    override fun release(decrement: Int): Boolean = byteBuf.release(decrement)
}
