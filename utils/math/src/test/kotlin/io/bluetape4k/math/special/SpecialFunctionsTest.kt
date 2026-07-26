package io.bluetape4k.math.special

import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sqrt

class SpecialFunctionsTest {

    companion object: KLogging()

    // ----- erf / erfc -----

    @Test
    fun `erf 0 에서 0 을 반환한다`() {
        0.0.erf().shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `erf 큰 양수에서 1 에 수렴한다`() {
        10.0.erf().shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `erf 홀함수이다`() {
        // erf(-x) = -erf(x)
        val x = 1.5
        (-x).erf().shouldBeNear(-x.erf(), 1e-10)
    }

    @Test
    fun `erfc 가 1 - erf 와 같다`() {
        val x = 1.0
        x.erfc().shouldBeNear(1.0 - x.erf(), 1e-10)
    }

    @Test
    fun `erfInv 가 erf 의 역함수이다`() {
        val x = 0.5
        x.erf().erfInv().shouldBeNear(x, 1e-10)
    }

    // ----- gamma / logGamma -----

    @Test
    fun `gamma 양의 정수에서 팩토리얼과 같다`() {
        // gamma(n) = (n-1)!
        1.0.gamma().shouldBeNear(1.0, 1e-10)   // 0!
        2.0.gamma().shouldBeNear(1.0, 1e-10)   // 1!
        3.0.gamma().shouldBeNear(2.0, 1e-10)   // 2!
        4.0.gamma().shouldBeNear(6.0, 1e-10)   // 3!
        5.0.gamma().shouldBeNear(24.0, 1e-10)  // 4!
    }

    @Test
    fun `gamma 0_5 에서 sqrt(PI) 와 같다`() {
        // gamma(1/2) = sqrt(π)
        0.5.gamma().shouldBeNear(sqrt(PI), 1e-10)
    }

    @Test
    fun `logGamma 가 ln(gamma) 와 같다`() {
        val x = 3.5
        x.logGamma().shouldBeNear(Math.log(x.gamma()), 1e-10)
    }

    // ----- digamma / trigamma -----

    @Test
    fun `digamma 1 에서 오일러 상수의 음수와 같다`() {
        1.0.digamma().shouldBeNear(-0.5772156649, 1e-8)
    }

    @Test
    fun `trigamma 1 에서 PI 제곱_6 과 같다`() {
        // trigamma(1) = π²/6
        1.0.trigamma().shouldBeNear(PI * PI / 6.0, 1e-8)
    }

    // ----- regularizedGammaP / regularizedGammaQ -----

    @Test
    fun `regularizedGammaP 와 regularizedGammaQ 의 합은 1 이다`() {
        val a = 2.0
        val x = 3.0
        val p = regularizedGammaP(a, x)
        val q = regularizedGammaQ(a, x)
        (p + q).shouldBeNear(1.0, 1e-10)
    }

    // ----- regularizedBeta -----

    @Test
    fun `regularizedBeta x 가 0 이면 0 을 반환한다`() {
        regularizedBeta(0.0, 1.0, 1.0).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `regularizedBeta x 가 1 이면 1 을 반환한다`() {
        regularizedBeta(1.0, 1.0, 1.0).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `regularizedBeta a 와 b 가 같으면 x=0_5 에서 0_5 를 반환한다`() {
        regularizedBeta(0.5, 2.0, 2.0).shouldBeNear(0.5, 1e-10)
    }

    // ----- logBeta -----

    @Test
    fun `logBeta B(1,1) 은 0 이다`() {
        1.0.logBeta(1.0).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `logBeta 대칭성이 성립한다`() {
        2.0.logBeta(3.0).shouldBeNear(3.0.logBeta(2.0), 1e-10)
    }

    // ----- erf(x2) 구간 오차 함수 -----

    @Test
    fun `erf 구간 함수가 erf 차와 같다`() {
        // erf(x1, x2) = erf(x2) - erf(x1)
        val x1 = 0.5
        val x2 = 1.5
        x1.erf(x2).shouldBeNear(x2.erf() - x1.erf(), 1e-10)
    }

    @Test
    fun `erf 구간 함수 x1=0 이면 erf(x2) 와 같다`() {
        val x2 = 1.0
        0.0.erf(x2).shouldBeNear(x2.erf(), 1e-10)
    }

    // ----- erfcInv -----

    @Test
    fun `erfcInv 가 erfc 의 역함수이다`() {
        val x = 0.5
        x.erfc().erfcInv().shouldBeNear(x, 1e-10)
    }

    @Test
    fun `erfcInv 1 에서 0 을 반환한다`() {
        1.0.erfcInv().shouldBeNear(0.0, 1e-10)
    }

    // ----- logGamma1p -----

    @Test
    fun `logGamma1p 가 logGamma(1 + x) 와 같다`() {
        // logGamma1p(x) accepts x in range [-0.5, 1.5]
        val x = 1.0
        x.logGamma1p().shouldBeNear((1.0 + x).logGamma(), 1e-10)
    }

    @Test
    fun `logGamma1p 0 에서 0 을 반환한다`() {
        // logGamma1p(0) = logGamma(1) = 0
        0.0.logGamma1p().shouldBeNear(0.0, 1e-10)
    }

    // ----- besselj -----

    @Test
    fun `besselj order 0 에서 x=0 이면 1 이다`() {
        0.0.besselj(0.0).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `besselj order 1 에서 x=0 이면 0 이다`() {
        0.0.besselj(1.0).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `besselj order 0 에서 알려진 값과 일치한다`() {
        // J_0(0) = 1, J_0(2) ≈ 0.22389 (양수)
        0.0.besselj(0.0).shouldBeNear(1.0, 1e-10)
        2.0.besselj(0.0).shouldBeNear(0.22389, 1e-4)
    }
}
