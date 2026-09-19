package io.bluetape4k.math.special

import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import kotlin.math.exp

class BetaFunctionsTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-10
    }

    // ----- betaLn -----

    @Test
    fun `betaLn 대칭성이 성립한다`() {
        betaLn(2.0, 3.0).shouldBeNear(betaLn(3.0, 2.0), EPSILON)
        betaLn(5.0, 2.0).shouldBeNear(betaLn(2.0, 5.0), EPSILON)
    }

    @Test
    fun `betaLn 알려진 값과 일치한다`() {
        // B(1,1) = 1, ln(B(1,1)) = 0
        betaLn(1.0, 1.0).shouldBeNear(0.0, EPSILON)

        // B(2,2) = 1/6, ln(B(2,2)) = ln(1/6)
        betaLn(2.0, 2.0).shouldBeNear(Math.log(1.0 / 6.0), EPSILON)
    }

    @Test
    fun `betaLn DoubleArray 오버로드가 동작한다`() {
        val xs = doubleArrayOf(1.0, 2.0, 3.0)
        val ys = doubleArrayOf(1.0, 2.0, 3.0)
        val result = betaLn(xs, ys)

        result shouldHaveSize 3
        result[0].shouldBeNear(betaLn(1.0, 1.0), EPSILON)
        result[1].shouldBeNear(betaLn(2.0, 2.0), EPSILON)
        result[2].shouldBeNear(betaLn(3.0, 3.0), EPSILON)
    }

    @Test
    fun `betaLn List 오버로드가 동작한다`() {
        val xs = listOf(1.0, 2.0)
        val ys = listOf(2.0, 3.0)
        val result = betaLn(xs, ys)

        result shouldHaveSize 2
        result[0].shouldBeNear(betaLn(1.0, 2.0), EPSILON)
        result[1].shouldBeNear(betaLn(2.0, 3.0), EPSILON)
    }

    // ----- beta -----

    @Test
    fun `beta 가 exp(betaLn) 과 일치한다`() {
        beta(2.0, 3.0).shouldBeNear(exp(betaLn(2.0, 3.0)), EPSILON)
        beta(5.0, 2.0).shouldBeNear(exp(betaLn(5.0, 2.0)), EPSILON)
    }

    @Test
    fun `beta B(1,1) 은 1 이다`() {
        beta(1.0, 1.0).shouldBeNear(1.0, EPSILON)
    }

    @Test
    fun `beta 대칭성이 성립한다`() {
        beta(2.0, 5.0).shouldBeNear(beta(5.0, 2.0), EPSILON)
    }

    @Test
    fun `beta DoubleArray 오버로드가 동작한다`() {
        val xs = doubleArrayOf(1.0, 2.0)
        val ys = doubleArrayOf(2.0, 3.0)
        val result = beta(xs, ys)

        result shouldHaveSize 2
        result[0].shouldBeNear(beta(1.0, 2.0), EPSILON)
        result[1].shouldBeNear(beta(2.0, 3.0), EPSILON)
    }

    @Test
    fun `beta List 오버로드가 동작한다`() {
        val xs = listOf(1.0, 2.0)
        val ys = listOf(1.0, 2.0)
        val result = beta(xs, ys)

        result shouldHaveSize 2
        result[0].shouldBeNear(beta(1.0, 1.0), EPSILON)
        result[1].shouldBeNear(beta(2.0, 2.0), EPSILON)
    }
}
