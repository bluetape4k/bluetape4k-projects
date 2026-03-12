package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class BinarySizeTest {

    companion object: KLogging()

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

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        // SI: 1 MB = 1000 kB
        val mb = 1.megabytes10()
        val asKb = mb `as` BinarySize.kiloBytes
        (asKb `in` BinarySize.kiloBytes).shouldBeNear(1000.0, 1e-10)

        // IEC: 1 MiB = 1024 KiB
        val mib = 1.mebiBytes()
        val asKib = mib `as` BinarySize.kibiBytes
        (asKib `in` BinarySize.kibiBytes).shouldBeNear(1024.0, 1e-10)

        // 8 bits = 1 byte
        val eightBits = 8.bits()
        val asBytes = eightBits `as` BinarySize.bytes
        (asBytes `in` BinarySize.bytes).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.kilobytes10() `in` BinarySize.bytes).shouldBeNear(1_000.0, 1e-10)
        (1.megabytes10() `in` BinarySize.kiloBytes).shouldBeNear(1_000.0, 1e-10)
        (1.gigabytes10() `in` BinarySize.megaBytes).shouldBeNear(1_000.0, 1e-10)
        // IEC
        (1.kibiBytes() `in` BinarySize.bytes).shouldBeNear(1_024.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `BinarySize 사칙연산이 동작한다`() {
        val a = 500.0.binaryBytes()
        val b = 1000.0.binaryBytes()

        // 덧셈
        (a + a) shouldBeEqualTo b
        // 뺄셈
        (b - a) shouldBeEqualTo a
        // 스칼라 곱셈
        (a * 2) shouldBeEqualTo b
        // 스칼라 나눗셈
        (b / 2) shouldBeEqualTo a
    }

    @Test
    fun `BinarySize 사칙연산 - 다른 단위 혼합`() {
        val bytes = 500.binaryBytes()
        val kb = 1.kilobytes10()

        val sum = bytes + kb
        (sum `in` BinarySize.bytes).shouldBeNear(1500.0, 1e-10)

        val diff = kb - bytes
        (diff `in` BinarySize.bytes).shouldBeNear(500.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `BinarySize 비교 연산이 동작한다`() {
        2.kilobytes10() shouldBeGreaterThan 1.kilobytes10()
        1.megabytes10() shouldBeGreaterThan 999.kilobytes10()
        1.kilobytes10() shouldBeLessThan 1001.binaryBytes()

        // SI vs IEC: 1 kB(1000) < 1 KiB(1024)
        1.kilobytes10() shouldBeLessThan 1.kibiBytes()
        // bits vs bytes: 8 bits == 1 byte, so 9 bits > 1 byte
        9.bits() shouldBeGreaterThan 1.binaryBytes()
    }

    // ----- 음수 단위 -----

    @Test
    fun `BinarySize 음수 단위가 동작한다`() {
        val positive = 1.kilobytes10()
        val negative = -positive

        (negative `in` BinarySize.bytes).shouldBeNear(-1000.0, 1e-10)

        val zero = positive + negative
        (zero `in` BinarySize.bytes).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `BinarySize toString 표현이 단위를 포함한다`() {
        1000.0.binaryBytes().toString() shouldBeEqualTo "1000.0 B"
        1.5.kilobytes10().toString() shouldBeEqualTo "1.5 kB"
        2.0.megabytes10().toString() shouldBeEqualTo "2.0 MB"
        1.0.kibiBytes().toString() shouldBeEqualTo "1.0 KiB"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.binaryBytes().toHuman(BinarySize.kiloBytes) shouldBeEqualTo "1.0 kB"
        1000.kilobytes10().toHuman(BinarySize.megaBytes) shouldBeEqualTo "1.0 MB"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.binaryBytes().toHuman() shouldBeEqualTo "500.0 B"
        1500.binaryBytes().toHuman() shouldBeEqualTo "1.5 kB"
        2000.kilobytes10().toHuman() shouldBeEqualTo "2.0 MB"
        3000.megabytes10().toHuman() shouldBeEqualTo "3.0 GB"
    }
}
