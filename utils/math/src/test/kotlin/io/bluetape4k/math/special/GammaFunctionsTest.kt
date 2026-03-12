package io.bluetape4k.math.special

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.ln

class GammaFunctionsTest {

    companion object: KLogging()

    // ----- gammaLowerRegularized -----

    @Test
    fun `gammaLowerRegularized x 가 0 이면 0 을 반환한다`() {
        gammaLowerRegularized(1.0, 0.0) shouldBeEqualTo 0.0
    }

    @Test
    fun `gammaLowerRegularized a 가 1 이면 1-exp(-x) 와 일치한다`() {
        // P(1, x) = 1 - exp(-x)
        gammaLowerRegularized(1.0, 1.0).shouldBeNear(1.0 - Math.exp(-1.0), 1e-10)
        gammaLowerRegularized(1.0, 2.0).shouldBeNear(1.0 - Math.exp(-2.0), 1e-10)
    }

    @Test
    fun `gammaLowerRegularized 와 gammaUpperRegularized 의 합은 1 이다`() {
        val a = 2.0
        val x = 3.0
        val lower = gammaLowerRegularized(a, x)
        val upper = gammaUpperRegularized(a, x)
        (lower + upper).shouldBeNear(1.0, 1e-10)
    }

    // ----- gammaUpperRegularized -----

    @Test
    fun `gammaUpperRegularized x 가 0 이하이면 1 을 반환한다`() {
        gammaUpperRegularized(1.0, 0.0) shouldBeEqualTo 1.0
        gammaUpperRegularized(1.0, -1.0) shouldBeEqualTo 1.0
    }

    @Test
    fun `gammaUpperRegularized 큰 x 에서 0 에 수렴한다`() {
        gammaUpperRegularized(1.0, 100.0).shouldBeNear(0.0, 1e-10)
    }

    // ----- gammaLn (DoubleArray extension) -----

    @Test
    fun `gammaLn DoubleArray 확장 함수가 동작한다`() {
        val input = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val result = input.gammaLn()

        result.size shouldBeEqualTo 4
        result[0].shouldBeNear(ln(1.0), 1e-10) // logGamma(1) = 0
        result[1].shouldBeNear(ln(1.0), 1e-10) // logGamma(2) = 0
        result[2].shouldBeNear(ln(2.0), 1e-10) // logGamma(3) = ln(2)
    }

    @Test
    fun `gammaLn Iterable 확장 함수가 동작한다`() {
        val input = listOf(1.0, 2.0, 3.0)
        val result = input.gammaLn()

        result.size shouldBeEqualTo 3
        result[0].shouldBeNear(0.0, 1e-10)
        result[1].shouldBeNear(0.0, 1e-10)
        result[2].shouldBeNear(ln(2.0), 1e-10)
    }

    // ----- gamma (DoubleArray extension) -----

    @Test
    fun `gamma DoubleArray 확장 함수가 동작한다`() {
        val input = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val result = input.gamma()

        result.size shouldBeEqualTo 4
        result[0].shouldBeNear(1.0, 1e-10)  // gamma(1) = 1
        result[1].shouldBeNear(1.0, 1e-10)  // gamma(2) = 1
        result[2].shouldBeNear(2.0, 1e-10)  // gamma(3) = 2
        result[3].shouldBeNear(6.0, 1e-10)  // gamma(4) = 6
    }

    @Test
    fun `gamma Iterable 확장 함수가 동작한다`() {
        val input = listOf(1.0, 2.0, 3.0)
        val result = input.gamma()

        result.size shouldBeEqualTo 3
        result[0].shouldBeNear(1.0, 1e-10)
        result[1].shouldBeNear(1.0, 1e-10)
        result[2].shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `gamma selector 오버로드가 동작한다`() {
        data class Item(val value: Double)

        val items = listOf(Item(1.0), Item(2.0), Item(3.0))
        val result = items.gamma { it.value }

        result.size shouldBeEqualTo 3
        result[0].shouldBeNear(1.0, 1e-10)
        result[1].shouldBeNear(1.0, 1e-10)
        result[2].shouldBeNear(2.0, 1e-10)
    }
}
