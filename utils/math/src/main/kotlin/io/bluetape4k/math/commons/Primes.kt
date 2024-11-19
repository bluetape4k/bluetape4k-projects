package io.bluetape4k.math.commons

import kotlin.math.sqrt

/**
 * 지정한 수가 소수인가 검사합니다.
 */
fun Long.isPrime(): Boolean {
    val sqrtValue = sqrt(this.toDouble()).toLong()

    for (i in 2 until sqrtValue) {
        if (this % i == 0L) {
            return false
        }
    }
    return true
}

/**
 * 지정한 수가 소수인가 검사합니다.
 */
fun Int.isPrime(): Boolean {
    val sqrtValue = sqrt(this.toDouble()).toInt()

    for (i in 2 until sqrtValue) {
        if (this % i == 0) {
            return false
        }
    }
    return true
}
