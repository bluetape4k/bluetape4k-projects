package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class PressureTest {
    @Test
    fun `압력 단위 변환이 동작한다`() {
        (1.atm() `in` Pressure.pascal).shouldBeNear(101_325.0, 1e-7)
        (1.bar() `in` Pressure.kiloPascal).shouldBeNear(100.0, 1e-10)
    }

    @Test
    fun `압력 toHuman 이 자동 단위를 선택한다`() {
        100_000.pascal().toHuman() shouldBeEqualTo "1.0 bar"
    }
}
