package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class AreaTest {
    @Test
    fun `길이 곱셈으로 면적을 계산한다`() {
        val area = 10.meters() * 2.meters()
        (area `in` Area.meters2).shouldBeNear(20.0, 1e-10)
    }

    @Test
    fun `면적 단위 변환이 동작한다`() {
        val squareMeter = 1.meters2()
        (squareMeter `in` Area.centimeters2).shouldBeNear(10_000.0, 1e-7)
    }

    @Test
    fun `면적 toHuman 이 자동 단위를 선택한다`() {
        10_000.centimeters2().toHuman() shouldBeEqualTo "1.0 m^2"
    }
}
