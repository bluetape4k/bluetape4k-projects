package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class BinarySizeTest {
    @Test
    fun `SI 바이트 단위 변환이 동작한다`() {
        (1.kilobytes10() `in` BinarySize.bytes).shouldBeNear(1_000.0, 1e-10)
        (1.gigabytes10() `in` BinarySize.megaBytes).shouldBeNear(1_000.0, 1e-10)
    }

    @Test
    fun `IEC 바이트 단위 변환이 동작한다`() {
        (1.kibiBytes() `in` BinarySize.bytes).shouldBeNear(1_024.0, 1e-10)
        (1.gibiBytes() `in` BinarySize.mebiBytes).shouldBeNear(1_024.0, 1e-10)
    }

    @Test
    fun `bit 와 byte 변환이 동작한다`() {
        (8.bits() `in` BinarySize.bytes).shouldBeNear(1.0, 1e-10)
        (1.binaryBytes() `in` BinarySize.bits).shouldBeNear(8.0, 1e-10)
    }

    @Test
    fun `BinarySize toHuman 이 자동 단위를 선택한다`() {
        1500.binaryBytes().toHuman() shouldBeEqualTo "1.5 kB"
    }
}
