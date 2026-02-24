package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class MassTest {
    @Test
    fun `질량 단위 변환이 동작한다`() {
        val ton = 2.tons()

        (ton `in` Mass.kilograms).shouldBeNear(2000.0, 1e-10)
        (ton `in` Mass.grams).shouldBeNear(2_000_000.0, 1e-5)
    }

    @Test
    fun `질량 toHuman 이 자동 단위를 선택한다`() {
        2500.grams().toHuman() shouldBeEqualTo "2.5 kg"
    }
}
