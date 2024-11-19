package io.bluetape4k.math.special

import io.bluetape4k.cache.memorizer.inmemory.InMemoryMemorizer
import io.bluetape4k.support.assertZeroOrPositiveNumber
import org.apache.commons.math3.special.Gamma.logGamma
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln

const val MAX_FACTORIAL_NUMBER = 170

class FactorialProvider {

    private val factorialCache = ConcurrentHashMap<Int, Double>()

    val cachedCalc: (Int) -> Double = InMemoryMemorizer { calc(it) }

    fun calc(n: Int): Double = when (n) {
        0, 1 -> 1.0
        else -> n * cachedCalc(n - 1)
    }
}

private val factorialProvider = FactorialProvider()

fun factorial(x: Int): Double {
    x.assertZeroOrPositiveNumber("x")
    assert(x < MAX_FACTORIAL_NUMBER) { "x[$x] must less than max factorial number [$MAX_FACTORIAL_NUMBER]" }

    if (x > MAX_FACTORIAL_NUMBER) {
        return Double.POSITIVE_INFINITY
    }
    return factorialProvider.calc(x)
}

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
