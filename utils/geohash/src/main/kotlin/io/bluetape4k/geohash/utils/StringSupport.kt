package io.bluetape4k.geohash.utils

fun padLeft(s: String, n: Int, pad: String): String {
    return String.format("%" + n + "s", s).replace(" ", pad)
}
