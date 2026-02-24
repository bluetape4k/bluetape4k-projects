package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class LengthTest {
    @Test
    fun `길이 단위 변환이 동작한다`() {
        val km = 1.kilometers()

        (km `in` Length.meters).shouldBeNear(1000.0, 1e-10)
        (km `in` Length.millimeters).shouldBeNear(1_000_000.0, 1e-6)
    }

    @Test
    fun `동일 계열 측정값 덧셈이 동작한다`() {
        val left = 500.meters()
        val right = 1.kilometers()

        val sum = left + right
        (sum `in` Length.meters).shouldBeNear(1500.0, 1e-10)
    }

    @Test
    fun `길이 toHuman 이 자동 단위를 선택한다`() {
        1500.meters().toHuman() shouldBeEqualTo "1.5 km"
    }
}
