package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.math.PI

@RandomizedTest
class MeasuredUnitsTest {
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
    fun `저장 용량 단위 변환이 동작한다`() {
        (1.gbytes() `in` Storage.megaBytes).shouldBeEqualTo(1024.0)
        (2048.bytes() `in` Storage.kiloBytes).shouldBeEqualTo(2.0)
    }

    @Test
    fun `압력 단위 변환이 동작한다`() {
        (1.atm() `in` Pressure.pascal).shouldBeNear(101_325.0, 1e-7)
        (1.bar() `in` Pressure.kiloPascal).shouldBeNear(100.0, 1e-10)
    }
}
