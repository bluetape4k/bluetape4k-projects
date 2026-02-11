@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import io.bluetape4k.utils.BigIntegerPair
import java.math.BigInteger
import java.util.*

@JvmField
val ZERO_UUID: UUID = 0L.toBigInteger().toUuid()

/**
 * [BigInteger]를 [UUID]로 변환합니다.
 *
 * ```
 * 123456789.toBigInteger().toUuid() // UUID(0, 123456789)
 * ```
 */
inline fun BigInteger.toUuid(): UUID {
    val (most, least) = BigIntegerPair.unpair(this)
    return UUID(most.longValueExact(), least.longValueExact())
}

/**
 * [UUID]를 [BigInteger]로 변환합니다.
 *
 * ```
 * UUID(0, 123456789).toBigInt() // 123456789.toBigInteger()
 * ```
 */
inline fun UUID.toBigInt(): BigInteger = BigIntegerPair.pair(
    this.mostSignificantBits.toBigInteger(),
    this.leastSignificantBits.toBigInteger()
)

/**
 * UUID의 구성요소인 `most significant bits` 와 `least significant bits`로 [LongArray]를 빌드합니다.
 *
 * ```
 * UUID(0, 123456789).toLongArray() // longArrayOf(0, 123456789)
 * ```
 */
inline fun UUID.toLongArray(): LongArray = longArrayOf(mostSignificantBits, leastSignificantBits)

/**
 * [LongArray]로 [UUID]를 빌드한다
 * index 0 가 mostSignificantBits, index 1이 leastSignificantBits 를 의미합니다.
 *
 * ```
 * longArrayOf(0, 123456789).toUUID() // UUID(0, 123456789)
 * ```
 */
inline fun LongArray.toUUID(): UUID {
    require(this.size >= 2) { "UUID need 2 long value" }
    return UUID(this[0], this[1])
}

/**
 * UUID 형식의 문자열 포맷을 나타내는 정규식입니다.
 */
@JvmField
val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

/**
 * 문자열이 UUID 형식인지 판단합니다.
 *
 * ```
 * "24738134-9d88-6645-4ec8-d63aa2031015".isUuid() // true
 * "24738134-9d88-6645-4ec8-d63aa2031015-43-ap".isUuid() // false
 * ```
 */
inline fun String.isUuid(): Boolean = UUID_REGEX.matches(this)
