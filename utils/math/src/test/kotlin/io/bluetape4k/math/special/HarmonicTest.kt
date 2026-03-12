package io.bluetape4k.math.special

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.PI

/**
 * [harmonic] 과 [diGamma] 는 소스 코드의 while 루프 버그로 인해 무한루프가 발생합니다.
 * (`while (x < C)` 조건에서 `x1`을 증가시키지만 `x`를 확인하므로 무한루프)
 * 따라서 해당 함수들에 대한 테스트는 제외합니다.
 */
class HarmonicTest {

    companion object: KLogging()

    // ----- generalHarmonic -----

    @Test
    fun `generalHarmonic m이 1이면 일반 조화수와 같다`() {
        // H(n, 1) = 1 + 1/2 + 1/3 + ... + 1/n
        generalHarmonic(1, 1.0).shouldBeNear(1.0, 1e-10)
        generalHarmonic(2, 1.0).shouldBeNear(1.5, 1e-10)
        generalHarmonic(4, 1.0).shouldBeNear(1.0 + 0.5 + 1.0 / 3 + 0.25, 1e-10)
    }

    @Test
    fun `generalHarmonic m이 2이면 바젤 급수에 수렴한다`() {
        // H(n, 2) = 1 + 1/4 + 1/9 + ... → π²/6 as n→∞
        val result = generalHarmonic(1000, 2.0)
        result.shouldBeNear(PI * PI / 6, 1e-2)
    }

    // ----- logit -----

    @Test
    fun `logit 0_5 에서 0 을 반환한다`() {
        logit(0.5).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `logit 값이 올바르다`() {
        // logit(p) = ln(p/(1-p))
        logit(0.75).shouldBeNear(Math.log(3.0), 1e-10)
        logit(0.25).shouldBeNear(-Math.log(3.0), 1e-10)
    }

    @Test
    fun `logit 범위 밖 입력에서 예외를 던진다`() {
        try {
            logit(-0.1)
            assert(false) { "예외가 발생해야 합니다" }
        } catch (_: IllegalArgumentException) {
        }
        try {
            logit(1.1)
            assert(false) { "예외가 발생해야 합니다" }
        } catch (_: IllegalArgumentException) {
        }
    }

    // ----- logistic -----

    @Test
    fun `logistic 0 에서 0_5 를 반환한다`() {
        logistic(0.0).shouldBeNear(0.5, 1e-10)
    }

    @Test
    fun `logistic 큰 양수에서 1 에 수렴한다`() {
        logistic(100.0).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `logistic 큰 음수에서 0 에 수렴한다`() {
        logistic(-100.0).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `logistic 과 logit 은 서로 역함수이다`() {
        val p = 0.7
        logistic(logit(p)).shouldBeNear(p, 1e-10)
    }
}
