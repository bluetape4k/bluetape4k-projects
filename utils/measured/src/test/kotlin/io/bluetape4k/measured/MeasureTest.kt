package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MeasureTest {
    private class CoreUnit(
        suffix: String,
        ratio: Double = 1.0,
    ): Units(suffix, ratio)

    @Test
    fun `toNearest가 동작한다`() {
        val unit = CoreUnit("u")
        val rounded = (10.26 * unit).toNearest(0.1)
        (rounded `in` unit).shouldBeNear(10.3, 1e-10)
    }

    @Test
    fun `문자열 표현이 단위를 포함한다`() {
        val unit = CoreUnit("u")
        val value = 42 * unit
        value.toString() shouldBeEqualTo "42.0 u"
    }
}
