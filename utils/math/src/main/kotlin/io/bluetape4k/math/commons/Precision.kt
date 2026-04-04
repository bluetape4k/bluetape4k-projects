package io.bluetape4k.math.commons

/**
 * Double 수형의 근사값의 최소량을 나타내는 Epsilon 을 구합니다.
 *
 * ```kotlin
 * 1.00001.epsilon() == 0.00001
 * ```
 *
 * @return Double 값의 epsilon(최소 표현 단위)
 */
fun Double.epsilon(): Double {
    if (this.isSpecialCase()) {
        return Double.NaN
    }

    var signed64 = this.toLong()

    if (signed64 == 0L) {
        signed64++
        return signed64.toDouble() - this
    }
    if (signed64 < 0L) {
        signed64--
        return signed64.toDouble() - this
    }

    return this - signed64.toDouble()
}

/**
 * Double 수형의 양의 epsilon(최소 표현 단위의 2배)을 구합니다.
 *
 * ```kotlin
 * val eps = 1.0.positiveEpsilon()   // Double.epsilon() * 2
 * ```
 */
fun Double.positiveEpsilon(): Double = 2.0 * this.epsilon()
