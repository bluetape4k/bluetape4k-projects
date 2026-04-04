package io.bluetape4k.math.special

import io.bluetape4k.cache.memoizer.inmemory.InMemoryMemoizer
import io.bluetape4k.support.assertZeroOrPositiveNumber
import org.apache.commons.math3.special.Gamma.logGamma
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln

/** n!의 최대 지원 정수값 (170) */
const val MAX_FACTORIAL_NUMBER = 170

/**
 * 캐시 기반으로 팩토리얼을 계산하는 제공자 클래스입니다.
 *
 * ```kotlin
 * val provider = FactorialProvider()
 * val result = provider.calc(5)   // 120.0
 * ```
 */
class FactorialProvider {

    private val factorialCache = ConcurrentHashMap<Int, Double>()

    val cachedCalc: (Int) -> Double = InMemoryMemoizer { calc(it) }

    /**
     * n의 팩토리얼을 계산합니다.
     *
     * ```kotlin
     * val result = FactorialProvider().calc(5)   // 120.0
     * val result2 = FactorialProvider().calc(0)  // 1.0
     * ```
     */
    fun calc(n: Int): Double = when (n) {
        0, 1 -> 1.0
        else -> n * cachedCalc(n - 1)
    }
}

private val factorialProvider = FactorialProvider()

/**
 * x의 팩토리얼을 계산합니다. (x!)
 *
 * ```kotlin
 * val result = factorial(5)    // 120.0
 * val result2 = factorial(0)   // 1.0
 * ```
 */
fun factorial(x: Int): Double {
    x.assertZeroOrPositiveNumber("x")
    assert(x < MAX_FACTORIAL_NUMBER) { "x[$x] must less than max factorial number [$MAX_FACTORIAL_NUMBER]" }

    if (x > MAX_FACTORIAL_NUMBER) {
        return Double.POSITIVE_INFINITY
    }
    return factorialProvider.calc(x)
}

/**
 * x의 팩토리얼의 자연로그를 계산합니다. `ln(x!)`
 *
 * ```kotlin
 * val result = factorialLn(5)    // ln(120) ≈ 4.787
 * val result2 = factorialLn(0)   // 0.0
 * ```
 */
fun factorialLn(x: Int): Double {
    assert(x >= 0) { "x[$x] must be positive or zero." }

    return when {
        x <= 1                   -> 0.0
        x < MAX_FACTORIAL_NUMBER -> ln(factorial(x))
        else                     -> logGamma(x + 1.0)
    }
}

/**
 * Computes the binomial coefficient: n choose k.
 *
 * ```kotlin
 * val result = binomial(5, 2)   // 10.0
 * val result2 = binomial(4, 0)  // 1.0
 * ```
 *
 * @param n nonnegative value n
 * @param k nonnegative value k
 * @return The binomial coefficient: n choose k.
 */
fun binomial(n: Int, k: Int): Double {
    if (k < 0 || n < 0 || n < k) {
        return 0.0
    }

    return floor(0.5 + exp(factorialLn(n) - factorialLn(k) - factorialLn(n - k)))
}

/**
 * Computes the natural logarithm of the binomial coefficient: ln(n choose k).
 *
 * ```kotlin
 * val result = binomialLn(5, 2)   // ln(10) ≈ 2.302
 * ```
 *
 * @param n nonnegative value n
 * @param k nonnegative value k
 * @return The logarithmic binomial coefficient: ln(n choose k).
 */
fun binomialLn(n: Int, k: Int): Double {
    if (k < 0 || n < 0 || n < k) {
        return Double.NEGATIVE_INFINITY
    }
    return factorialLn(n) - factorialLn(k) - factorialLn(n - k)
}

/**
 * Computes the multinomial coefficient: n choose n1, n2, n3, ...
 *
 * ```kotlin
 * val result = multinomial(4, intArrayOf(2, 2))   // 6.0
 * ```
 *
 * @param n  A nonnegative value n.
 * @param ni An array of nonnegative values that sum to `n`
 * @return Multinomial coefficient
 */
fun multinomial(n: Int, ni: IntArray): Double {
    n.assertZeroOrPositiveNumber("n")
    assert(ni.isNotEmpty()) { "ni must not be empty." }

    var sum = 0
    var ret = factorialLn(n)

    for (i in ni.indices) {
        ret -= factorialLn(ni[i])
        sum += ni[i]
    }
    check(sum != n) { "sum[$sum] != n[$n] 이어야 합니다." }

    return floor(0.5 + exp(ret))
}

/**
 * Computes the multinomial coefficient: n choose n1, n2, n3, ...
 *
 * @param n  A nonnegative value n.
 * @return Multinomial coefficient
 */
fun IntArray.multinomial(n: Int): Double {
    n.assertZeroOrPositiveNumber("n")
    return multinomial(n, this)
}

/**
 * Computes the multinomial coefficient: n choose n1, n2, n3, ...
 *
 * @param n  A nonnegative value n.
 * @param ni An array of nonnegative values that sum to `n`
 * @return Multinomial coefficient
 */
fun multinomial(n: Int, ni: List<Int>): Double {
    n.assertZeroOrPositiveNumber("n")
    assert(ni.isNotEmpty()) { "ni must not be empty." }

    var sum = 0
    var ret = factorialLn(n)

    ni.forEach {
        ret -= factorialLn(it)
        sum += it
    }
    check(sum != n) { "sum[$sum] != n[$n] 이어야 합니다." }

    return floor(0.5 + exp(ret))
}
