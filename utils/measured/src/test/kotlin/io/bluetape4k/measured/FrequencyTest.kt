package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class FrequencyTest {

    companion object: KLogging()

    @Test
    fun `주파수 단위 변환이 동작한다`() {
        (1.gigaHertz() `in` Frequency.megaHertz).shouldBeNear(1000.0, 1e-10)
        (1000.hertz() `in` Frequency.kiloHertz).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `주파수 toHuman 이 자동 단위를 선택한다`() {
        1500.hertz().toHuman() shouldBeEqualTo "1.5 kHz"
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val ghz = 1.gigaHertz()
        val asMhz = ghz `as` Frequency.megaHertz
        (asMhz `in` Frequency.megaHertz).shouldBeNear(1000.0, 1e-10)

        val khz = 1.kiloHertz()
        val asHz = khz `as` Frequency.hertz
        (asHz `in` Frequency.hertz).shouldBeNear(1000.0, 1e-10)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.gigaHertz() `in` Frequency.hertz).shouldBeNear(1_000_000_000.0, 1e-1)
        (1.megaHertz() `in` Frequency.kiloHertz).shouldBeNear(1000.0, 1e-10)
        (1.kiloHertz() `in` Frequency.hertz).shouldBeNear(1000.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `주파수 사칙연산이 동작한다`() {
        val a = 500.0.hertz()
        val b = 1000.0.hertz()

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
    fun `주파수 사칙연산 - 다른 단위 혼합`() {
        val hz = 500.hertz()
        val khz = 1.kiloHertz()

        val sum = hz + khz
        (sum `in` Frequency.hertz).shouldBeNear(1500.0, 1e-10)

        val diff = khz - hz
        (diff `in` Frequency.hertz).shouldBeNear(500.0, 1e-10)
    }

    // ----- 비교 -----

    @Test
    fun `주파수 비교 연산이 동작한다`() {
        2.gigaHertz() shouldBeGreaterThan 1.gigaHertz()
        1.megaHertz() shouldBeGreaterThan 999.kiloHertz()
        1.kiloHertz() shouldBeLessThan 1001.hertz()

        // 다른 단위 간 비교
        1.gigaHertz() shouldBeGreaterThan 999.megaHertz()
        1.megaHertz() shouldBeGreaterThan 1.kiloHertz()
    }

    // ----- 음수 단위 -----

    @Test
    fun `주파수 음수 단위가 동작한다`() {
        val positive = 1.kiloHertz()
        val negative = -positive

        (negative `in` Frequency.hertz).shouldBeNear(-1000.0, 1e-10)

        val zero = positive + negative
        (zero `in` Frequency.hertz).shouldBeNear(0.0, 1e-10)
    }

    // ----- toString 표현 -----

    @Test
    fun `주파수 toString 표현이 단위를 포함한다`() {
        440.0.hertz().toString() shouldBeEqualTo "440.0 Hz"
        1.5.kiloHertz().toString() shouldBeEqualTo "1.5 kHz"
        2.4.gigaHertz().toString() shouldBeEqualTo "2.4 GHz"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        1000.hertz().toHuman(Frequency.kiloHertz) shouldBeEqualTo "1.0 kHz"
        1000.kiloHertz().toHuman(Frequency.megaHertz) shouldBeEqualTo "1.0 MHz"
        1000.megaHertz().toHuman(Frequency.gigaHertz) shouldBeEqualTo "1.0 GHz"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.hertz().toHuman() shouldBeEqualTo "500.0 Hz"
        1500.hertz().toHuman() shouldBeEqualTo "1.5 kHz"
        2500.kiloHertz().toHuman() shouldBeEqualTo "2.5 MHz"
        3500.megaHertz().toHuman() shouldBeEqualTo "3.5 GHz"
    }
}
