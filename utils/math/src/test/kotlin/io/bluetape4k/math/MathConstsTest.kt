package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.PI
import io.bluetape4k.assertions.assertFailsWith

class MathConstsTest {

    companion object: KLogging()

    @Test
    fun `Pi 상수가 올바르다`() {
        MathConsts.Pi.shouldBeNear(PI, 1e-15)
        MathConsts.Pi2.shouldBeNear(2.0 * PI, 1e-15)
        MathConsts.PiOver2.shouldBeNear(PI / 2.0, 1e-15)
    }

    @Test
    fun `pow2가 올바른 값을 반환한다`() {
        MathConsts.pow2(0) shouldBeEqualTo 1
        MathConsts.pow2(1) shouldBeEqualTo 2
        MathConsts.pow2(3) shouldBeEqualTo 8
        MathConsts.pow2(10) shouldBeEqualTo 1024
        MathConsts.pow2(15) shouldBeEqualTo 32768
    }

    @Test
    fun `pow2 범위 밖 인덱스는 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            MathConsts.pow2(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            MathConsts.pow2(16)
        }
    }

    @Test
    fun `goldenRatio가 올바르다`() {
        MathConsts.goldenRatio.shouldBeNear(1.6180339887, 1e-9)
    }

    @Test
    fun `EPSILON이 양수이다`() {
        (MathConsts.EPSILON > 0.0) shouldBeEqualTo true
        (MathConsts.FLOAT_EPSILON > 0.0f) shouldBeEqualTo true
    }
}
