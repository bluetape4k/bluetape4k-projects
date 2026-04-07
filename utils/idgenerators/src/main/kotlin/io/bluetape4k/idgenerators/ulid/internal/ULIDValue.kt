package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.ULID
import io.bluetape4k.logging.KLogging
import java.io.Serializable

data class ULIDValue(
    override val mostSignificantBits: Long,
    override val leastSignificantBits: Long,
): ULID,
   Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override val timestamp: Long
        get() = mostSignificantBits ushr 16

    override fun toByteArray(): ByteArray {
        val bytes = ByteArray(16)
        (0..7).forEach { bytes[it] = (mostSignificantBits shr ((7 - it) * 8) and Mask8Bits).toByte() }
        (8..15).forEach { bytes[it] = (leastSignificantBits shr ((15 - it) * 8) and Mask8Bits).toByte() }
        return bytes
    }

    override fun increment(): ULID =
        when {
            leastSignificantBits != AllBitsSet -> {
                ULIDValue(mostSignificantBits, leastSignificantBits + 1)
            }
            (mostSignificantBits and Mask16Bits) != Mask16Bits -> {
                ULIDValue(mostSignificantBits + 1, 0)
            }
            else                               -> {
                ULIDValue(mostSignificantBits and TimestampMsbMask, 0)
            }
        }

    override fun compareTo(other: ULID): Int {
        val msbCmp = mostSignificantBits.toULong().compareTo(other.mostSignificantBits.toULong())
        if (msbCmp != 0) return msbCmp
        return leastSignificantBits.toULong().compareTo(other.leastSignificantBits.toULong())
    }

    override fun toString(): String {
        val buffer = CharArray(26)
        buffer.write(timestamp, 10, 0)
        var value = (mostSignificantBits and Mask16Bits) shl 24
        val interim = leastSignificantBits ushr 40
        value = value or interim
        buffer.write(value, 8, 10)
        buffer.write(leastSignificantBits, 8, 18)
        return buffer.concatToString()
    }
}
