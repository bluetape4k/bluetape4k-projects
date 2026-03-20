package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.ULID
import kotlin.random.Random

internal class ULIDFactory(
    private val random: Random = Random,
) : ULID.Factory {
    companion object {
        val Default = ULIDFactory()
    }

    override fun randomULID(timestamp: Long): String {
        requireTimestamp(timestamp)

        val bytes = random.nextBytes(10)
        val buffer = CharArray(26)
        buffer.write(timestamp, 10, 0)
        buffer.write(bytes.toLong(0, 5), 8, 10)
        buffer.write(bytes.toLong(5, 10), 8, 18)

        return buffer.concatToString()
    }

    override fun nextULID(timestamp: Long): ULID {
        requireTimestamp(timestamp)
        val bytes = random.nextBytes(10)
        val mostSignificantBits = bytes.toLong(0, 2) or (timestamp shl 16)
        val leastSignificantBits = bytes.toLong(2, 10)
        return ULIDValue(mostSignificantBits, leastSignificantBits)
    }

    override fun fromByteArray(data: ByteArray): ULID {
        require(data.size == 16) { "data must be 16 bytes in length" }

        var mostSignificantBits = 0L
        var leastSignificantBits = 0L

        (0..7).forEach { mostSignificantBits = (mostSignificantBits shl 8) or (data[it].toLong() and Mask8Bits) }
        (8..15).forEach { leastSignificantBits = (leastSignificantBits shl 8) or (data[it].toLong() and Mask8Bits) }

        return ULIDValue(mostSignificantBits, leastSignificantBits)
    }

    override fun parseULID(ulidString: String): ULID {
        require(ulidString.length == 26) { "ulid string must be exactly 26 chars long" }

        val timeString = ulidString.substring(0, 10)
        val time = timeString.parseCrockford()

        require((time and TimestampOverflowMask) == 0L) {
            "ulid string must not exceed '7ZZZZZZZZZZZZZZZZZZZZZZZZZ'!"
        }

        val part1String = ulidString.substring(10, 18)
        val part2String = ulidString.substring(18)
        val part1 = part1String.parseCrockford()
        val part2 = part2String.parseCrockford()

        val most = (time shl 16) or (part1 ushr 24)
        val least = part2 or (part1 shl 40)

        return ULIDValue(most, least)
    }
}
