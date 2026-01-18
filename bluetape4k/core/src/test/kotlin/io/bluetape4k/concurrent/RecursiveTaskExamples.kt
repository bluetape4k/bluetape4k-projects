package io.bluetape4k.concurrent

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.concurrent.RecursiveTask

/**
 * ForkJoin Pool을 사용하는 [RecursiveTask] 이용하여 재귀작업을 빠르게 수행하는 예제입니다.
 *
 * 병렬, 분산처리 방식도 있지만, 이렇게 재귀호출의 경우에는 ForkJoin Pool을 사용하는 것이 더 효율적입니다.
 */
class RecursiveTaskExamples {

    @Test
    fun `RecursiveTask 를 이용하여 Factorial 계산하기`() {
        Factorial.factorial(0) shouldBeEqualTo 1.toBigInteger()
        Factorial.factorial(1) shouldBeEqualTo 1.toBigInteger()
        Factorial.factorial(2) shouldBeEqualTo 2.toBigInteger()
        Factorial.factorial(3) shouldBeEqualTo 6.toBigInteger()
        Factorial.factorial(4) shouldBeEqualTo 24.toBigInteger()
    }

    @Test
    fun `RecursiveTask 를 이용하여 Factorial 계산하기 - Large number`() {
        Factorial.factorial(10) shouldBeEqualTo 3628800.toBigInteger()
    }

    object Factorial {
        class FactorialTask(
            private val fromValue: Int,
            private val toValue: Int,
        ): RecursiveTask<BigInteger>() {

            override fun compute(): BigInteger {
                return when (val range = toValue - fromValue) {
                    0    -> fromValue.toBigInteger()
                    1    -> fromValue.toBigInteger() * toValue.toBigInteger()
                    else -> {
                        val mid = fromValue + range / 2
                        val leftTask = FactorialTask(fromValue, mid)
                        leftTask.fork()  // perform about half the work locally
                        FactorialTask(mid + 1, toValue).compute() * leftTask.join()
                    }
                }
            }
        }

        fun factorial(n: Int): BigInteger {
            if (n <= 1) {
                return BigInteger.ONE
            }
            return FactorialTask(1, n).invoke()
        }
    }
}
