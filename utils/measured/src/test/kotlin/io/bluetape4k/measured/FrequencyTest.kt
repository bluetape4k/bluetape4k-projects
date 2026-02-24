package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class FrequencyTest {
    @Test
    fun `주파수 단위 변환이 동작한다`() {
        (1.gigaHertz() `in` Frequency.megaHertz).shouldBeNear(1000.0, 1e-10)
        (1000.hertz() `in` Frequency.kiloHertz).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `주파수 toHuman 이 자동 단위를 선택한다`() {
        1500.hertz().toHuman() shouldBeEqualTo "1.5 kHz"
    }
}
