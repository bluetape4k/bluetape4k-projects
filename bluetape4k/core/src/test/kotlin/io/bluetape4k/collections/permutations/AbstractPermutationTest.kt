package io.bluetape4k.collections.permutations

import io.bluetape4k.logging.KLogging

/**
 * Permutation 테스트를 위한 추상 기반 클래스입니다.
 *
 * 공통 fixture와 유틸리티 메서드를 제공합니다.
 */
abstract class AbstractPermutationTest {

    companion object: KLogging()

    protected val emptyPermutation: Permutation<Any> get() = emptyPermutation()
    protected val emptyIntPermutation: Permutation<Int> get() = emptyPermutation()

    protected val expectedList = listOf(3, -2, 8, 5, -4, 11, 2, 1)

    protected fun lazy(): Permutation<Int> {
        return cons(3) {
            cons(-2) {
                cons(8) {
                    cons(5) {
                        cons(-4) {
                            cons(11) {
                                cons(2) {
                                    permutationOf(1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun lazy2() = expectedList.toPermutation()

    protected fun loremIpsum(): Array<String> {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi id metus at ligula convallis imperdiet. "
            .lowercase().split("[ \\.,]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }
}

/**
 * 소수(Prime number) 순열을 생성합니다.
 *
 * @return 2, 3, 5, 7, 11, ... 소수의 무한 순열
 */
fun primes(): Permutation<Int> {
    return iterate(2, ::nextPrimeAfter)
}

private fun nextPrimeAfter(after: Int): Int {
    var candidate = after + 1
    while (!isPrime(candidate)) {
        candidate++
    }
    return candidate
}

private fun isPrime(candidate: Int): Boolean {
    val max = Math.sqrt(candidate.toDouble()).toInt()
    for (div in 2..max) {
        if (candidate % div == 0) {
            return false
        }
    }
    return true
}
