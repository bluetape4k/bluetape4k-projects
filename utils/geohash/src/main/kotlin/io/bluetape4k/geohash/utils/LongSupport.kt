package io.bluetape4k.geohash.utils

private val FIRST_BIT: Long = (0x8000000000000000U).toLong()

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
