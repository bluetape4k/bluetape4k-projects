package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class EnergyPowerTest {
    @Test
    fun `에너지 단위 변환이 동작한다`() {
        (1.kiloWattHours() `in` Energy.joules).shouldBeNear(3_600_000.0, 1e-5)
        (3_600.joules() `in` Energy.wattHours).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `전력과 시간으로 에너지를 계산한다`() {
        val energy = 2.kiloWatts() * 3.hours()
        (energy `in` Energy.kiloWattHours).shouldBeNear(6.0, 1e-10)

        val power = energy / 2.hours()
        (power `in` Power.kiloWatts).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `에너지 전력 toHuman 이 자동 단위를 선택한다`() {
        1500.joules().toHuman() shouldBeEqualTo "1.5 kJ"
        1500.watts().toHuman() shouldBeEqualTo "1.5 kW"
    }
}
