package io.bluetape4k.support

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * [Number]를 [BigDecimal]로 변환합니다.
 */
fun Number.toBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is Long       -> BigDecimal(this)
    else          -> BigDecimal(this.asDouble().toString())
}

/**
 * [BigDecimal] 들을 더합니다.
 * ```
 * 42.toBigDecimal() + 3.toBigDecimal()  // 45.toBigDecimal()
 * ```
 */
operator fun BigDecimal.plus(other: Number): BigDecimal = this.add(other.toBigDecimal())

/**
 * [BigDecimal] 들을 뺍니다.
 * ```
 * 42.toBigDecimal() - 3.toBigDecimal()  // 39.toBigDecimal()
 * ```
 */
operator fun BigDecimal.minus(other: Number): BigDecimal = this.subtract(other.toBigDecimal())

/**
 * [BigDecimal] 들을 곱합니다.
 * ```
 * 42.toBigDecimal() * 2.toBigDecimal()  // 84.toBigDecimal()
 * ```
 */
operator fun BigDecimal.times(other: Number): BigDecimal = this.multiply(other.toBigDecimal())

/**
 * [BigDecimal] 들을 나눕니다.
 * ```
 * 42.toBigDecimal() / 2.toBigDecimal()  // 21.toBigDecimal()
 * ```
 */
operator fun BigDecimal.div(other: Number): BigDecimal = this.divide(other.toBigDecimal())

/**
 * [BigDecimal] 들을 비교합니다.
 * ```
 * 42.toBigDecimal() > 5.toBigDecimal()   // true
 * 42.toBigDecimal() < 10.toBigDecimal()  // false
 * ```
 */
operator fun BigDecimal.compareTo(other: Number): Int = this.compareTo(other.toBigDecimal())

/**
 * 둘을 곱합니다.
 * ```
 * 4 * 3.toBigDecimal()  // 12.toBigDecimal()
 * ```
 */
operator fun <T: Number> T.times(other: BigDecimal): BigDecimal = other.times(this)

/**
 * BigDecimal의 모든 요소를 더한다
 */
@JvmName("sumOfBigDecimal")
fun Iterable<BigDecimal>.sum(): BigDecimal {
    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * BigDecimal 컬렉션의 평균을 구합니다.
 */
@JvmName("averageOfBigDecimal")
fun Iterable<BigDecimal>.average(): BigDecimal {
    var sum = BigDecimal.ZERO
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return sum / count
}

/**
 * BigDecimal을 반올림합니다.
 */
fun BigDecimal.roundUp(scale: Int = 0, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal {
    return this.setScale(scale, roundingMode)
}
