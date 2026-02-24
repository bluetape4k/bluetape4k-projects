package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.PI

@RandomizedTest
class AngleTest {
    @Test
    fun `각도 단위 변환이 동작한다`() {
        (180.degrees() `in` Angle.radians).shouldBeNear(PI, 1e-10)
        (PI.radians() `in` Angle.degrees).shouldBeNear(180.0, 1e-10)
    }

    @Test
    fun `각도 정규화가 동작한다`() {
        (450.degrees().normalize() `in` Angle.degrees).shouldBeNear(90.0, 1e-10)
        ((-30).degrees().normalize() `in` Angle.degrees).shouldBeNear(330.0, 1e-10)
    }

    @Test
    fun `각도 toHuman 이 정규화된 degree 로 표시된다`() {
        450.degrees().toHuman() shouldBeEqualTo "90.0°"
    }
}
