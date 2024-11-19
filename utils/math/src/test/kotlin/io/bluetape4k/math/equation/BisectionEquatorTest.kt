package io.bluetape4k.math.equation

import org.junit.jupiter.api.Assumptions

class BisectionEquatorTest: AbstractEquatorTest() {

    override val equator: Equator = BisectionEquator()

    override fun `경계가 같은 + 부호를 가지는 경우`() {
        Assumptions.assumeFalse(false, "Bisection 방식은 경계 값이 같은 부호라면 수행할 수 없습니다.")
    }

    override fun `경계가 같은 - 부호를 가지는 경우`() {
        Assumptions.assumeFalse(false, "Bisection 방식은 경계 값이 같은 부호라면 수행할 수 없습니다.")
    }
}
