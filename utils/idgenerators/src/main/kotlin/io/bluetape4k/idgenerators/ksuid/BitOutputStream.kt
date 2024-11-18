package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireZeroOrPositiveNumber

class BitOutputStream private constructor(val capacity: Int) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(capacity: Int): BitOutputStream {
            capacity.requireZeroOrPositiveNumber("capacity")
            return BitOutputStream(capacity)
        }
    }

    private val bytes: ByteArray = ByteArray(capacity / Byte.SIZE_BITS)
    private var offset: Int = 0

    private fun currentBit(): Int = offset % Byte.SIZE_BITS
    private fun currentLength(): Int = offset / Byte.SIZE_BITS

    fun bitsCountUpToByte(): Int = when (val currBit = currentBit()) {
        0    -> 0
        else -> Byte.SIZE_BITS - currBit
    }

    fun toArray(): ByteArray = when (val currLen = currentLength()) {
        bytes.size -> bytes.copyOf()
        else       -> bytes.copyOf(currLen)
    }

    fun writeBits(bitsCount: Int, bits: Int) {
        val bitNum = currentBit()
        val byteNum = currentLength()

        val firstWrite = minOf(Byte.SIZE_BITS - bitNum, bitsCount)
        val secondWrite = bitsCount - firstWrite

        bytes[byteNum] = (bytes[byteNum].toInt() or (bits and (1 shl firstWrite) - 1 shl bitNum)).toByte()
        if (secondWrite > 0) {
            bytes[byteNum + 1] =
                (bytes[byteNum + 1].toInt() or (bits ushr firstWrite and (1 shl secondWrite) - 1)).toByte()
        }
        offset += bitsCount
        log.trace { "Wrote $bitsCount bits from offset ${offset - bitsCount} to $offset: $bits" }
    }
}
