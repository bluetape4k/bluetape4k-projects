package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireInRange

internal class BitInputStream(private val bytes: ByteArray) {

    companion object: KLogging()

    private val bitLength: Int = bytes.size * Byte.SIZE_BITS // 8 bits per byte
    private var offset: Int = 0

    fun hasMore(): Boolean = offset < bitLength

    fun seekBit(pos: Int) {
        offset += pos
        if (offset !in 0..bitLength) {
            throw IndexOutOfBoundsException("Invalid offset. offset=$offset, pos=$pos")
        }
    }

    fun readBits(bitsCount: Int): Int {
        bitsCount.requireInRange(1, Byte.SIZE_BITS, "bitsCount")

        val bitNum = offset % Byte.SIZE_BITS
        val byteNum = offset / Byte.SIZE_BITS

        val firstRead = minOf(Byte.SIZE_BITS - bitNum, bitsCount)
        val secondRead = bitsCount - firstRead

        var result = (bytes[byteNum].toInt() and ((1 shl firstRead) - 1 shl bitNum)).ushr(bitNum)
        if (secondRead > 0 && bytes.size > byteNum + 1) {
            result = result or (bytes[byteNum + 1].toInt() and ((1 shl secondRead) - 1) shl firstRead)
        }
        offset += bitsCount
        log.trace { "Read $bitsCount bits from offset ${offset - bitsCount} to $offset: $result" }

        return result
    }
}
