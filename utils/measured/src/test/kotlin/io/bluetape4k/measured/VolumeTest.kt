package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class VolumeTest {
    @Test
    fun `부피 단위 변환이 동작한다`() {
        (1.liters() `in` Volume.milliliters).shouldBeNear(1000.0, 1e-10)
        (1.cubicMeters() `in` Volume.liters).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `면적과 길이로 부피를 계산한다`() {
        val volume = 10.meters2() * 2.meters()
        (volume `in` Volume.cubicMeters).shouldBeNear(20.0, 1e-10)
    }

    @Test
    fun `부피 toHuman 이 자동 단위를 선택한다`() {
        1500.milliliters().toHuman() shouldBeEqualTo "1.5 L"
    }
}
