package io.bluetape4k.geohash.utils

/**
 * 첫 번째 비트 플래그 (최상위 비트가 1인 Long 값)
 */
private val FIRST_BIT: Long = (0x8000000000000000U).toLong()

/**
 * 두 Long 값의 공통 접두사 길이를 계산합니다.
 *
 * 최상위 비트부터 비교하여 서로 다른 비트가 나타날 때까지의 비트 수를 반환합니다.
 *
 * ```
 * val a = 0xF000000000000000L
 * val b = 0xF800000000000000L
 * a.commonPrefixLength(b)  // 4
 * ```
 *
 * @param other 비교할 다른 Long 값
 * @return 공통 접두사의 비트 수 (0~64)
 */
fun Long.commonPrefixLength(other: Long): Int {
    var result = 0
    var v1 = this
    var v2 = other

    while (result < 64 && (v1 and FIRST_BIT) == (v2 and FIRST_BIT)) {
        result++
        v1 = v1 shl 1
        v2 = v2 shl 1
    }
    return result
}
