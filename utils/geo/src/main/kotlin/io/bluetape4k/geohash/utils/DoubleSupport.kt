package io.bluetape4k.geohash.utils

/**
 * [remainder]로 나머지 연산을 수행하고, 음수일 경우 [remainder]를 더해 양수로 만든다.
 *
 * ```
 * 58.1541.remainderWithFix(360).shouldBeNear(58.1541, DELTA)
 * 453.1541.remainderWithFix(360).shouldBeNear(93.1541, DELTA)
 *
 * (-58.1541).remainderWithFix(360).shouldBeNear(301.8459, DELTA)
 * (-453.1541).remainderWithFix(360).shouldBeNear(266.8459, DELTA)
 * ```
 */
fun Double.remainderWithFix(remainder: Int): Double {
    val res = this % remainder

    return if (res < 0) {
        res + remainder
    } else {
        res
    }
}
