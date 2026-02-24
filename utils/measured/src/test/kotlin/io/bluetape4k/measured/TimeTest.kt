package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class TimeTest {
    @Test
    fun `시간 기반 복합 단위 계산이 동작한다`() {
        val speed: Measure<Velocity> = 10.meters() / 2.seconds()
        val distance = speed * 6.seconds()

        (distance `in` Length.meters).shouldBeNear(30.0, 1e-10)
    }

    @Test
    fun `시간 toHuman 이 자동 단위를 선택한다`() {
        120000.milliseconds().toHuman() shouldBeEqualTo "2.0 min"
    }
}
