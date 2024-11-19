package io.bluetape4k.math.commons

/**
 * 3제곱근 계산
 */
fun cubeRoot(v: Double): Double {
    if (v.approximateEqual(0.0)) {
        return 0.0
    }

    var x = v
    var prev: Double
    val positive = x > 0.0

    if (!positive) {
        x = -x
    }
    var s = if (x > 1) x else 1.0
    do {
        prev = s
        s = (x / (s * s) + 2.0 * s) / 3.0
    } while (s < prev)

    return if (positive) prev else -prev
}
