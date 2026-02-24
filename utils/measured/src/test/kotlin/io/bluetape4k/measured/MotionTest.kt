package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MotionTest {
    @Test
    fun `속도 확장 함수와 변환이 동작한다`() {
        val speed = 36.kilometersPerHour()
        speed.inMetersPerSecond().shouldBeNear(10.0, 1e-10)
    }

    @Test
    fun `가속도 확장 함수와 변환이 동작한다`() {
        val acceleration = 9.8.metersPerSecondSquared()
        acceleration.inMetersPerSecondSquared().shouldBeNear(9.8, 1e-10)
    }
}
