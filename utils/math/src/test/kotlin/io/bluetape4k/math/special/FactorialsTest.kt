package io.bluetape4k.math.special

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.ln
import kotlin.test.assertFailsWith

class FactorialsTest {

    companion object: KLogging()

    // ----- factorial -----

    @Test
    fun `factorial 0과 1은 1을 반환한다`() {
        factorial(0) shouldBeEqualTo 1.0
        factorial(1) shouldBeEqualTo 1.0
    }

    @Test
    fun `factorial 이 올바른 값을 반환한다`() {
        factorial(2) shouldBeEqualTo 2.0
        factorial(3) shouldBeEqualTo 6.0
        factorial(4) shouldBeEqualTo 24.0
        factorial(5) shouldBeEqualTo 120.0
        factorial(10).shouldBeNear(3628800.0, 1e-5)
        factorial(20).shouldBeNear(2.43290200817664e18, 1e5)
    }

    @Test
    fun `factorial 최대값 경계에서 동작한다`() {
        factorial(169) shouldBeGreaterThan 0.0
    }

    @Test
    fun `factorial 음수 입력에서 예외를 던진다`() {
        assertFailsWith<AssertionError> {
            factorial(-1)
        }
    }

    // ----- factorialLn -----

    @Test
    fun `factorialLn 0과 1은 0을 반환한다`() {
        factorialLn(0) shouldBeEqualTo 0.0
        factorialLn(1) shouldBeEqualTo 0.0
    }

    @Test
    fun `factorialLn 이 ln(factorial) 과 일치한다`() {
        factorialLn(5).shouldBeNear(ln(factorial(5)), 1e-10)
        factorialLn(10).shouldBeNear(ln(factorial(10)), 1e-10)
    }

    // ----- binomial -----

    @Test
    fun `binomial 기본값을 반환한다`() {
        binomial(5, 0) shouldBeEqualTo 1.0
        binomial(5, 5) shouldBeEqualTo 1.0
        binomial(5, 1) shouldBeEqualTo 5.0
        binomial(5, 2) shouldBeEqualTo 10.0
        binomial(5, 3) shouldBeEqualTo 10.0
        binomial(5, 4) shouldBeEqualTo 5.0
    }

    @Test
    fun `binomial 음수 또는 잘못된 입력은 0을 반환한다`() {
        binomial(-1, 0) shouldBeEqualTo 0.0
        binomial(0, -1) shouldBeEqualTo 0.0
        binomial(3, 5) shouldBeEqualTo 0.0
    }

    @Test
    fun `binomial 대칭성이 성립한다`() {
        // C(n,k) == C(n, n-k)
        binomial(10, 3) shouldBeEqualTo binomial(10, 7)
        binomial(8, 2) shouldBeEqualTo binomial(8, 6)
    }

    // ----- binomialLn -----

    @Test
    fun `binomialLn 이 ln(binomial) 과 일치한다`() {
        binomialLn(10, 3).shouldBeNear(ln(binomial(10, 3)), 1e-10)
        binomialLn(20, 5).shouldBeNear(ln(binomial(20, 5)), 1e-10)
    }

    @Test
    fun `binomialLn 잘못된 입력은 NEGATIVE_INFINITY를 반환한다`() {
        binomialLn(-1, 0) shouldBeEqualTo Double.NEGATIVE_INFINITY
        binomialLn(3, 5) shouldBeEqualTo Double.NEGATIVE_INFINITY
    }

}
