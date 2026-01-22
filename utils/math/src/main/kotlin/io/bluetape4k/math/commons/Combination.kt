package io.bluetape4k.math.commons

import io.bluetape4k.math.special.binomial
import io.bluetape4k.math.special.factorial
import io.bluetape4k.math.special.factorialLn
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow

/**
 * 순열에서 조합을 구한다.
 *
 * @param n 모집단의 갯수
 * @param k 선택할 갯수
 * @return 조합
 */
fun combination(n: Int, k: Int): Int {
    if (k <= 0 || n == k)
        return 1

    return combination(n - 1, k - 1) + combination(n - 1, k)
}

/**
 * 순열에서 조합을 구한다.
 *
 * @param n 모집단의 갯수
 * @param k 선택할 갯수 (주의: k가 17보다 크면 예외를 발생시킵니다)
 * @return 조합
 */
fun longCombination(n: Int, k: Int): Long {
    if (k <= 0) {
        return 1L
    }
    assert(k <= 17) { "k [$k] is two large number than 17." }

    val a = LongArray(17)

    var m = k
    if (n - k < k) {
        m = n - k
    }

    if (m == 0) return 1L
    if (m == 1) return n.toLong()

    assert(k <= 17) { "k [$k] is two large number than 17." }

    for (i in 1..m) {
        a[i] = i.toLong() + 2L
    }

    for (i in 3..(n - m + 1)) {
        a[0] = i.toLong()
        for (j in 1 until m) {
            a[j] += a[j - 1]
        }
    }
    return a[m - 1]
}

/**
 * Counts the number of possible variations without repetition.
 * The order matters and each object can be chosen only once.
 *
 * @param n Number of elements in the set.
 * @param k Number of elements to choose from the set. Each element is chosen at most once.
 * @return Maximum number of distinct variations.
 */
fun variations(n: Int, k: Int): Double {
    if (k < 0 || n < 0 || n < k) {
        return 0.0
    }
    return floor(0.5 + exp(factorialLn(n) - factorialLn(n - k)))
}

/**
 * Counts the number of possible variations with repetition.
 * The order matters and each object can be chosen more than once.
 *
 * @param n Number of elements in the set.
 * @param k Number of elements to choose from the set. Each element is chosen 0, 1 or multiple times.
 * @return Maximum number of distinct variations with repetition.
 */
fun variationsWithRepetition(n: Int, k: Int): Double {
    if (n < 0 || k < 0) {
        return 0.0
    }
    return n.toDouble().pow(k.toDouble())
}

/**
 * Counts the number of possible combinations without repetition.
 * The order does not matter and each object can be chosen only once.
 *
 * @param n Number of elements in the set.
 * @param k Number of elements to choose from the set. Each element is chosen at most once.
 * @return Maximum number of combinations.
 */
fun combinations(n: Int, k: Int): Double {
    return binomial(n, k)
}

/**
 * Counts the number of possible combinations with repetition.
 * The order does not matter and an object can be chosen more than once.
 *
 * @param n Number of elements in the set.
 * @param k Number of elements to choose from the set. Each element is chosen 0, 1 or multiple times.
 * @return Maximum number of combinations with repetition.
 */
fun combinationsWithRepetition(n: Int, k: Int): Double {
    if (n < 0 || k < 0 || (n == 0 && k > 0)) {
        return 0.0
    }
    if (n == 0 && k == 0) {
        return 1.0
    }

    return floor(0.5 + exp(factorialLn(n + k - 1) - factorialLn(k) - factorialLn(n - 1)))
}

/**
 * Counts the number of possible permutations (without repetition).
 *
 * @param n  Number of (distinguishable) elements in the set.
 * @return  Maximum number of permutations without repetition.
 */
fun permutations(n: Int): Double = factorial(n)
