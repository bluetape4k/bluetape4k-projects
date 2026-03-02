@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import java.math.BigInteger

/**
 * 숫자 값을 [BigInteger]로 변환합니다.
 *
 * ## 동작/계약
 * - [value]가 정수/실수/BigInteger 중 무엇이든 [toBigInt] 규칙으로 변환합니다.
 * - null 허용 입력이 아니며, 변환 불가 타입은 받지 않습니다.
 * - 수신 객체를 변경하지 않고 새로운 값(또는 기존 BigInteger)을 반환합니다.
 * - 변환 비용은 입력 타입에 따라 상수 시간 또는 문자열 변환 비용이 발생합니다.
 *
 * ```kotlin
 * val n1 = bigIntOf(10)
 * val n2 = bigIntOf(42L)
 * // n1 + n2 == 52.toBigInt()
 * ```
 *
 * @param value 변환할 숫자 값
 */
inline fun <T: Number> bigIntOf(value: T): BigInteger = value.toBigInt()

/**
 * bigIntArrayOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = bigIntArrayOf(1, 2, 3)
 * // result.size == 3
 * ```
 */
inline fun <T: Number> bigIntArrayOf(vararg values: T): Array<BigInteger> =
    Array(values.size) { i -> values[i].toBigInt() }

/**
 * bigIntListOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = bigIntListOf(1, 2, 3)
 * // result == [1, 2, 3]
 * ```
 */
inline fun <T: Number> bigIntListOf(vararg values: T): List<BigInteger> =
    List(values.size) { i -> values[i].toBigInt() }

/**
 * bigIntArray 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = bigIntArray(listOf(1, 2, 3))
 * // result.size == 3
 * ```
 */
inline fun <T: Number> bigIntArray(size: Int, init: (Int) -> T): Array<BigInteger> {
    size.requirePositiveNumber("size")
    return Array(size) { init(it).toBigInt() }
}

/**
 * bigIntList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = bigIntList(listOf(1, 2, 3))
 * // result == [1, 2, 3]
 * ```
 */
inline fun <T: Number> bigIntList(size: Int, init: (Int) -> T): List<BigInteger> {
    size.requirePositiveNumber("size")
    return List(size) { init(it).toBigInt() }
}


/**
 * [Number]를 [BigInteger]로 변환합니다.
 */
fun <T: Number> T.toBigInt(): BigInteger = when (this) {
    is BigInteger             -> this
    is Int, is Short, is Byte -> toLong().toBigInteger()
    is Long                   -> toBigInteger()
    else                      -> toLong().toBigInteger() 
}

/**
 * 10진수 정수 문자열을 [BigInteger]로 변환합니다.
 *
 * ## 동작/계약
 * - 입력은 `BigInteger(String)` 생성 규칙(부호 포함 10진수)을 따릅니다.
 * - blank/whitespace 문자열은 허용되지 않으며 [NumberFormatException]이 발생할 수 있습니다.
 * - 수신 문자열은 변경되지 않으며 새 [BigInteger]를 반환합니다.
 * - 문자열 길이에 비례한 파싱 비용이 발생합니다.
 *
 * ```kotlin
 * val value = "12345678901234567890".toBigInt()
 * // value.signum() > 0
 * ```
 */
fun String.toBigInt(): BigInteger = BigInteger(this)


/**
 * [BigInteger] 들을 더합니다.
 * 
 * ```
 * 42.toBigInt() + 3.toBigInt()  // 45.toBigInt()
 * ```
 */
operator fun <T: Number> BigInteger.plus(other: T): BigInteger = this.add(other.toBigInt())

/**
 * [BigInteger] 들을 뺍니다.
 *
 * ```
 * 42.toBigInt() - 3.toBigInt()  // 39.toBigInt()
 * ```
 */
operator fun <T: Number> BigInteger.minus(other: T): BigInteger = this.subtract(other.toBigInt())

/**
 * [BigInteger] 들을 곱합니다.
 *
 * ```
 * 42.toBigInt() * 2  // 84.toBigInt()
 * ```
 */
operator fun <T: Number> BigInteger.times(other: T): BigInteger = this.multiply(other.toBigInt())

/**
 * [BigInteger] 들을 곱합니다.
 *
 * ```
 * 42 * 2.toBigInt()  // 84.toBigInt()
 * ```
 */
operator fun <T: Number> T.times(other: BigInteger): BigInteger = other.times(this)

/**
 * [BigInteger] 들을 나눕니다.
 *
 * ```
 * 12.toBigInt() / 3.toBigInt()  // 4.toBigInt()
 * ```
 */
operator fun <T: Number> BigInteger.div(other: T): BigInteger = this.divide(other.toBigInt())

/**
 * [BigInteger] 들을 나누고, 나머지와 몫을 반환합니다.
 *
 * ```
 * 42.toBigInt().divideAndRemainder(25)  // (1.toBigInt(), 17.toBigInt())
 * ```
 */
fun <T: Number> BigInteger.divideAndRemainder(other: T): Pair<BigInteger, BigInteger> {
    val results = this.divideAndRemainder(other.toBigInt())
    return results[0] to results[1]
}

/**
 * [BigInteger]와 일반 숫자 타입을 비교합니다.
 *
 * ## 동작/계약
 * - [other]를 [toBigInt]로 변환한 뒤 [BigInteger.compareTo]를 수행합니다.
 * - null 입력은 허용되지 않습니다.
 * - 수신 객체를 변경하지 않으며 새 컬렉션/배열을 만들지 않습니다.
 * - 시간 복잡도는 자릿수에 비례합니다.
 *
 * ```kotlin
 * // 10.toBigInt() > 2
 * // 10.toBigInt().compareTo(10L) == 0
 * ```
 *
 * @param other 비교 대상 숫자
 */
operator fun BigInteger.compareTo(other: Number): Int = this.compareTo(other.toBigInt())

/**
 * [BigInteger] 컬렉션의 모든 요소의 합을 구합니다.
 */
@JvmName("sumOfBigIntIterable")
fun Iterable<BigInteger>.sum(): BigInteger {
    if (this.none())
        return BigInteger.ZERO

    var sum = BigInteger.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * [BigInteger] 컬렉션의 모든 요소의 합을 구합니다.
 */
@JvmName("sumOfBigIntArray")
fun Array<BigInteger>.sum(): BigInteger {
    if (this.none())
        return BigInteger.ZERO

    var sum = BigInteger.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

/**
 * [BigInteger] 컬렉션의 평균값을 구합니다.
 */
@JvmName("averageOfBigIntIterable")
fun Iterable<BigInteger>.average(): Double {
    if (this.none())
        return 0.0

    var sum = BigInteger.ZERO
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return sum.toDouble() / count
}

/**
 * [BigInteger] Array 의 평균값을 구합니다.
 */
@JvmName("averageOfBigIntArray")
fun Array<BigInteger>.average(): Double {
    if (this.none())
        return 0.0

    var sum = BigInteger.ZERO
    var count = 0
    for (element in this) {
        sum += element
        count++
    }
    return sum.toDouble() / count
}
