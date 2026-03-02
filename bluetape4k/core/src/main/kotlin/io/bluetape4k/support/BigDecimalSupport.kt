@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 숫자 값을 [BigDecimal]로 변환합니다.
 *
 * ## 동작/계약
 * - [value] 타입에 따라 [toBigDecimal] 규칙으로 변환합니다.
 * - null 입력은 허용되지 않습니다.
 * - 수신 값을 변경하지 않으며 새 값(또는 동일 인스턴스)을 반환합니다.
 * - 문자열 경유 변환이 필요한 타입은 추가 파싱 비용이 발생할 수 있습니다.
 *
 * ```kotlin
 * val a = bigDecimalOf(10)
 * val b = bigDecimalOf(2.5)
 * check(a + b == BigDecimal("12.5"))
 * ```
 *
 * @param value 변환할 숫자 값
 */
fun <T: Number> bigDecimalOf(value: T): BigDecimal = value.toBigDecimal()

/**
 * bigDecimalArrayOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::bigDecimalArrayOf
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Number> bigDecimalArrayOf(vararg values: T): Array<BigDecimal> =
    Array(values.size) { i -> values[i].toBigDecimal() }

/**
 * bigDecimalListOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::bigDecimalListOf
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Number> bigDecimalListOf(vararg values: T): List<BigDecimal> =
    List(values.size) { i -> values[i].toBigDecimal() }

/**
 * bigDecimalArray 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::bigDecimalArray
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Number> bigDecimalArray(size: Int, init: (Int) -> T): Array<BigDecimal> {
    size.requirePositiveNumber("size")
    return Array(size) { init(it).toBigDecimal() }
}

/**
 * bigDecimalList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::bigDecimalList
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Number> bigDecimalList(size: Int, init: (Int) -> T): List<BigDecimal> {
    size.requirePositiveNumber("size")
    return List(size) { init(it).toBigDecimal() }
}


/**
 * [Number]를 [BigDecimal]로 변환합니다.
 */
fun <T: Number> T.toBigDecimal(): BigDecimal = when (this) {
    is BigDecimal             -> this
    is Int, is Short, is Byte -> BigDecimal(this.toLong())
    is Long                   -> BigDecimal(this)
    else -> BigDecimal(this.toString())
}

/**
 * [BigDecimal] 들을 더합니다.
 * ```
 * 42.toBigDecimal() + 3.toBigDecimal()  // 45.toBigDecimal()
 * ```
 */
operator fun <T: Number> BigDecimal.plus(other: T): BigDecimal = this.add(other.toBigDecimal())

/**
 * [BigDecimal] 들을 뺍니다.
 * ```
 * 42.toBigDecimal() - 3.toBigDecimal()  // 39.toBigDecimal()
 * ```
 */
operator fun <T: Number> BigDecimal.minus(other: T): BigDecimal = this.subtract(other.toBigDecimal())

/**
 * [BigDecimal] 들을 곱합니다.
 * ```
 * 42.toBigDecimal() * 2.toBigDecimal()  // 84.toBigDecimal()
 * ```
 */
operator fun <T: Number> BigDecimal.times(other: T): BigDecimal = this.multiply(other.toBigDecimal())


/**
 * 둘을 곱합니다.
 *
 * ```
 * 4 * 3.toBigDecimal()  // 12.toBigDecimal()
 * ```
 */
operator fun <T: Number> T.times(other: BigDecimal): BigDecimal = other.times(this)


/**
 * 두 [BigDecimal] 값을 나눕니다.
 *
 * ⚠️ 결과가 무한 소수가 되는 경우 [ArithmeticException]을 발생시킵니다.
 * 정확히 나누어 떨어지는 경우에만 사용하세요.
 *
 * 반올림이 필요한 경우에는 [divideSafe]를 사용하세요.
 */
operator fun <T: Number> BigDecimal.div(other: T): BigDecimal = this.divide(other.toBigDecimal())

/**
 * 두 [BigDecimal] 값을 나눕니다.
 *
 * ```kotlin
 * 10.toBigDecimal().divideSafe(3)            // 3.33.toBigDecimal()
 * 10.toBigDecimal().divideSafe(3, 4)         // 3.3333.toBigDecimal()
 * 10.toBigDecimal().divideSafe(3, 4, RoundingMode.DOWN)  // 3.3333.toBigDecimal()
 * ```
 * @param scale: 소수점 이하 자릿수 (기본값: 2)
 * @param roundingMode: 반올림 모드 (기본값: RoundingMode.HALF_UP)
 * @return 나눗셈 결과를 반올림한 [BigDecimal]
 */
fun BigDecimal.divideSafe(
    other: Number,
    scale: Int = 2,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
): BigDecimal =
    divide(other.toBigDecimal(), scale, roundingMode)

/**
 * [BigDecimal] 들을 비교합니다.
 *
 * ```
 * 42.toBigDecimal() > 5.toBigDecimal()   // true
 * 42.toBigDecimal() < 10.toBigDecimal()  // false
 * ```
 */
operator fun BigDecimal.compareTo(other: Number): Int = this.compareTo(other.toBigDecimal())

/**
 * BigDecimal을 반올림합니다.
 */
fun BigDecimal.roundUp(
    scale: Int = 2,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
): BigDecimal =
    setScale(scale, roundingMode)

/**
 * BigDecimal의 모든 요소를 더한다
 */
@JvmName("sumOfBigDecimalIterable")
fun Iterable<BigDecimal>.sum(): BigDecimal {
    if (this.none())
        return BigDecimal.ZERO

    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * BigDecimal의 모든 요소를 더한다
 */
@JvmName("sumOfBigDecimalArray")
fun Array<BigDecimal>.sum(): BigDecimal {
    if (this.none())
        return BigDecimal.ZERO

    var sum = BigDecimal.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * BigDecimal 컬렉션의 평균을 구합니다.
 */
@JvmName("averageOfBigDecimalIterable")
fun Iterable<BigDecimal>.average(
    scale: Int = 2,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
): BigDecimal {
    if (this.none())
        return BigDecimal.ZERO

    var sum = BigDecimal.ZERO
    var count = 0
    for (element in this) {
        sum += element
        count++
    }

    return sum.divideSafe(count, scale, roundingMode)
}

/**
 * BigDecimal Array의 평균을 구합니다.
 */
@JvmName("averageOfBigDecimalArray")
fun Array<BigDecimal>.average(
    scale: Int = 2,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
): BigDecimal {
    if (this.none())
        return BigDecimal.ZERO

    var sum = BigDecimal.ZERO
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return sum.divideSafe(count, scale, roundingMode)
}
