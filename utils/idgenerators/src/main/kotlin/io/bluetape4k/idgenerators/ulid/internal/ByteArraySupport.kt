package io.bluetape4k.idgenerators.ulid.internal

internal fun ByteArray.toLong(
    from: Int,
    to: Int,
): Long {
    var result = 0L
    for (i in from until to) {
        result = (result shl 8) or (this[i].toLong() and Mask8Bits)
    }
    return result
}
