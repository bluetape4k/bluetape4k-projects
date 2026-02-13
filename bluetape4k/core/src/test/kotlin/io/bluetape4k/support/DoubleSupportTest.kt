package io.bluetape4k.support

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class DoubleSupportTest {

    @Test
    fun `Double isFinite는 유한 값만 true를 반환한다`() {
        0.0.isFinite.shouldBeTrue()
        (-1234.5678).isFinite.shouldBeTrue()

        Double.NaN.isFinite.shouldBeFalse()
        Double.POSITIVE_INFINITY.isFinite.shouldBeFalse()
        Double.NEGATIVE_INFINITY.isFinite.shouldBeFalse()
    }

    @Test
    fun `Float isFinite는 유한 값만 true를 반환한다`() {
        0.0F.isFinite.shouldBeTrue()
        1234.5678F.isFinite.shouldBeTrue()

        Float.NaN.isFinite.shouldBeFalse()
        Float.POSITIVE_INFINITY.isFinite.shouldBeFalse()
        Float.NEGATIVE_INFINITY.isFinite.shouldBeFalse()
    }
}
