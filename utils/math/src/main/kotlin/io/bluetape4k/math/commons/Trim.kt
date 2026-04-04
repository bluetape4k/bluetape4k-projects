package io.bluetape4k.math.commons

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/**
 * 특정 자릿수에 대해 올림을 수행합니다.
 *
 * ```kotlin
 * val result = 1.234.ceilDigits(2)    // 1.24
 * val result2 = 1.230.ceilDigits(2)   // 1.23
 * ```
 *
 * @param digits 올림 자릿 수
 * @return 올림을 수행한 값
 */
fun Double.ceilDigits(digits: Int = 0): Double {
    if (this.isSpecialCase() || isMaxValue() || isMinValue()) {
        return ceil(this)
    }
    val x = this * 10.0.pow(digits)
    return ceil((x * 10.0).toLong() / 10.0) / 10.0.pow(digits)
}

/**
 * 특정 자릿수에 대해 내림을 수행합니다.
 *
 * ```kotlin
 * val result = 1.239.floorDigits(2)   // 1.23
 * val result2 = 3456.0.floorDigits(-2)   // 3400.0
 * ```
 *
 * @param digits 내림 자릿 수
 * @return 내림을 수행한 값
 */
fun Double.floorDigits(digits: Int = 0): Double {
    if (this.isSpecialCase() || isMaxValue() || isMinValue()) {
        return floor(this)
    }
    val x = this * 10.0.pow(digits)
    return floor((x * 10.0).toLong() / 10.0) / 10.0.pow(digits)
}

/**
 * 특정 자릿수에 대해 반올림합니다.
 * [반올림 공식 IEEE 754](https://ko.wikipedia.org/wiki/IEEE_754)
 *
 * ```
 * 12.4525.rount(3)    // returns 12.452
 * 3456.0.round(-2)      // returns 3400.0
 * 3556.0.round(-2)      // returns 3600.0
 * ```
 *
 * @param digits 반올림할 자릿수
 * @return 반올림된 숫자
 */
fun Double.roundDigits(digits: Int = 0): Double {
    if (this.isSpecialCase() || isMaxValue() || isMinValue()) {
        return round(this)
    }
    val x = this * 10.0.pow(digits)
    return round((x * 10.0).toLong() / 10.0) / 10.0.pow(digits)
}

/**
 * 컬렉션 요소들의 값의 올림을 합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.231, 2.459).ceilDigits(2).toList()
 * // [1.24, 2.46]
 * ```
 */
fun Sequence<Double>.ceilDigits(digits: Int = 0): Sequence<Double> = map { it.ceilDigits(digits) }

/**
 * 컬렉션 요소들의 값의 올림을 합니다.
 *
 * ```kotlin
 * val result = listOf(1.231, 2.459).ceilDigits(2).toList()
 * // [1.24, 2.46]
 * ```
 */
fun Iterable<Double>.ceilDigits(digits: Int = 0): Iterable<Double> = map { it.ceilDigits(digits) }


/**
 * 컬렉션 요소들의 값의 내림을 합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.239, 2.456).floorDigits(2).toList()
 * // [1.23, 2.45]
 * ```
 */
fun Sequence<Double>.floorDigits(digits: Int = 0): Sequence<Double> = map { it.floorDigits(digits) }

/**
 * 컬렉션 요소들의 값의 내림을 합니다.
 *
 * ```kotlin
 * val result = listOf(1.239, 2.456).floorDigits(2).toList()
 * // [1.23, 2.45]
 * ```
 */
fun Iterable<Double>.floorDigits(digits: Int = 0): Iterable<Double> = map { it.floorDigits(digits) }


/**
 * 컬렉션 요소들의 값의 반올림을 합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.234, 2.455).roundDigits(2).toList()
 * // [1.23, 2.46]
 * ```
 */
fun Sequence<Double>.roundDigits(digits: Int = 0): Sequence<Double> = map { it.roundDigits(digits) }

/**
 * 컬렉션 요소들의 값의 반올림을 합니다.
 *
 * ```kotlin
 * val result = listOf(1.234, 2.455).roundDigits(2).toList()
 * // [1.23, 2.46]
 * ```
 */
fun Iterable<Double>.roundDigits(digits: Int = 0): Iterable<Double> = map { it.roundDigits(digits) }

/**
 * DoubleArray의 요소를 지정 자릿수로 올림하여 제자리에서 변경합니다.
 *
 * ```kotlin
 * val arr = doubleArrayOf(1.231, 2.459)
 * arr.ceilDigitsThis(2)
 * // arr == [1.24, 2.46]
 * ```
 */
fun DoubleArray.ceilDigitsThis(digits: Int = 0) {
    forEachIndexed { index, value ->
        this[index] = value.ceilDigits(digits)
    }
}

/**
 * DoubleArray의 요소를 지정 자릿수로 내림하여 제자리에서 변경합니다.
 *
 * ```kotlin
 * val arr = doubleArrayOf(1.239, 2.456)
 * arr.floorDigitsThis(2)
 * // arr == [1.23, 2.45]
 * ```
 */
fun DoubleArray.floorDigitsThis(digits: Int = 0) {
    forEachIndexed { index, value ->
        this[index] = value.floorDigits(digits)
    }
}

/**
 * DoubleArray의 요소를 지정 자릿수로 반올림하여 제자리에서 변경합니다.
 *
 * ```kotlin
 * val arr = doubleArrayOf(1.234, 2.455)
 * arr.roundDigitsThis(2)
 * // arr == [1.23, 2.46]
 * ```
 */
fun DoubleArray.roundDigitsThis(digits: Int = 0) {
    forEachIndexed { index, value ->
        this[index] = value.roundDigits(digits)
    }
}
