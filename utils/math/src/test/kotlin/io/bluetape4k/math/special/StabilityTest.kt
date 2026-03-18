package io.bluetape4k.math.special

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class StabilityTest {

    companion object: KLogging()

    // ----- exponentialMinusOne -----

    @Test
    fun `exponentialMinusOne 0 에서 0 을 반환한다`() {
        exponentialMinusOne(0.0).shouldBeNear(0.0, 1e-15)
    }

    @Test
    fun `exponentialMinusOne 작은 값에서 수치 안정성이 보장된다`() {
        // 작은 x: exp(x)-1 ≈ x (1차 근사)
        val x = 1e-8
        exponentialMinusOne(x).shouldBeNear(x, 1e-15)
    }

    @Test
    fun `exponentialMinusOne 큰 값에서 exp(x)-1 과 일치한다`() {
        val x = 2.0
        exponentialMinusOne(x).shouldBeNear(Math.exp(x) - 1.0, 1e-10)
    }

    @Test
    fun `exponentialMinusOne 음수 입력에서도 동작한다`() {
        val x = -0.5
        exponentialMinusOne(x).shouldBeNear(Math.exp(x) - 1.0, 1e-10)
    }

    // ----- hypotenuse -----

    @Test
    fun `hypotenuse 피타고라스 정리와 일치한다`() {
        // 3, 4, 5 직각삼각형
        hypotenuse(3.0, 4.0).shouldBeNear(5.0, 1e-10)
    }

    @Test
    fun `hypotenuse 동일한 두 변이면 sqrt(2) x a 이다`() {
        val a = 1.0
        hypotenuse(a, a).shouldBeNear(sqrt(2.0) * a, 1e-10)
    }

    @Test
    fun `hypotenuse 한 변이 0 이면 다른 변의 절대값을 반환한다`() {
        hypotenuse(5.0, 0.0).shouldBeNear(5.0, 1e-10)
        hypotenuse(0.0, 3.0).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `hypotenuse 교환법칙이 성립한다`() {
        hypotenuse(3.0, 4.0).shouldBeNear(hypotenuse(4.0, 3.0), 1e-10)
        hypotenuse(5.0, 12.0).shouldBeNear(hypotenuse(12.0, 5.0), 1e-10)
    }

    @Test
    fun `hypotenuse 5-12-13 직각삼각형`() {
        hypotenuse(5.0, 12.0).shouldBeNear(13.0, 1e-10)
    }
}
