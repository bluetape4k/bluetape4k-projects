@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import java.math.BigInteger

inline fun <T: Number> bigIntOf(value: T): BigInteger = value.toBigInt()

inline fun <T: Number> bigIntArrayOf(vararg values: T): Array<BigInteger> =
    Array(values.size) { i -> values[i].toBigInt() }

inline fun <T: Number> bigIntListOf(vararg values: T): List<BigInteger> =
    List(values.size) { i -> values[i].toBigInt() }

inline fun <T: Number> bigIntArray(size: Int, init: (Int) -> T): Array<BigInteger> {
    size.requirePositiveNumber("size")
    return Array(size) { init(it).toBigInt() }
}

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
