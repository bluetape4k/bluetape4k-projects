package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.ULID
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * ULID 생성 팩토리입니다.
 *
 * ## 보안 주의
 * - 기본값은 [SecureRandom] 기반의 Kotlin Random을 사용합니다.
 * - 세션 ID, 보안 토큰 등 예측 불가능성이 필요한 경우 반드시 [SecureRandom] 기반의 인스턴스를 사용하세요.
 * - 성능이 중요한 비보안 컨텍스트에서만 `kotlin.random.Random.Default`를 직접 전달하세요.
 */
internal class ULIDFactory(
    private val random: Random = SecureRandom().asKotlinRandom(),
): ULID.Factory {
    companion object {
        val Default by lazy { ULIDFactory() }
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
