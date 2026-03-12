package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class PressureTest {

    companion object: KLogging()

    @Test
    fun `압력 단위 변환이 동작한다`() {
        (1.atm() `in` Pressure.pascal).shouldBeNear(101_325.0, 1e-7)
        (1.bar() `in` Pressure.kiloPascal).shouldBeNear(100.0, 1e-10)
    }

    @Test
    fun `압력 toHuman 이 자동 단위를 선택한다`() {
        100_000.pascal().toHuman() shouldBeEqualTo "1.0 bar"
    }

    // ----- 단위 변환 -----

    @Test
    fun `as 연산자로 다른 단위로 변환한다`() {
        val bar = 1.bar()
        val asKpa = bar `as` Pressure.kiloPascal
        (asKpa `in` Pressure.kiloPascal).shouldBeNear(100.0, 1e-10)

        val atm = 1.atm()
        val asPa = atm `as` Pressure.pascal
        (asPa `in` Pressure.pascal).shouldBeNear(101_325.0, 1e-7)
    }

    @Test
    fun `in 연산자로 단위 수치값을 추출한다`() {
        (1.bar() `in` Pressure.pascal).shouldBeNear(100_000.0, 1e-5)
        (1.atm() `in` Pressure.kiloPascal).shouldBeNear(101.325, 1e-5)
        (1.megaPascal() `in` Pressure.kiloPascal).shouldBeNear(1000.0, 1e-10)
        (100.kiloPascal() `in` Pressure.bar).shouldBeNear(1.0, 1e-10)
    }

    // ----- 사칙 연산 -----

    @Test
    fun `압력 사칙연산이 동작한다`() {
        val a = 50.0.kiloPascal()
        val b = 100.0.kiloPascal()

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
    fun `압력 사칙연산 - 다른 단위 혼합`() {
        val pa = 50_000.pascal()
        val kpa = 100.kiloPascal()

        val sum = pa + kpa
        (sum `in` Pressure.pascal).shouldBeNear(150_000.0, 1e-8)

        val diff = kpa - pa
        (diff `in` Pressure.pascal).shouldBeNear(50_000.0, 1e-8)
    }

    // ----- 비교 -----

    @Test
    fun `압력 비교 연산이 동작한다`() {
        2.bar() shouldBeGreaterThan 1.bar()
        1.atm() shouldBeGreaterThan 1.bar()   // 101325 Pa > 100000 Pa
        1.megaPascal() shouldBeGreaterThan 1.bar()

        // 다른 단위 간 비교
        1.bar() shouldBeLessThan 1.atm()
        1.kiloPascal() shouldBeLessThan 1.bar()
        10.psi() shouldBeLessThan 1.bar()     // ~68948 Pa < 100000 Pa
    }

    // ----- 음수 단위 -----

    @Test
    fun `압력 음수 단위가 동작한다`() {
        val positive = 1.bar()
        val negative = -positive

        (negative `in` Pressure.pascal).shouldBeNear(-100_000.0, 1e-5)

        val zero = positive + negative
        (zero `in` Pressure.pascal).shouldBeNear(0.0, 1e-5)
    }

    // ----- toString 표현 -----

    @Test
    fun `압력 toString 표현이 단위를 포함한다`() {
        100.0.kiloPascal().toString() shouldBeEqualTo "100.0 kPa"
        1.0.bar().toString() shouldBeEqualTo "1.0 bar"
        1.0.atm().toString() shouldBeEqualTo "1.0 atm"
    }

    // ----- toHuman 표현 -----

    @Test
    fun `toHuman 에 특정 단위를 지정할 수 있다`() {
        100_000.pascal().toHuman(Pressure.bar) shouldBeEqualTo "1.0 bar"
        101_325.pascal().toHuman(Pressure.atmosphere) shouldBeEqualTo "1.0 atm"
        100_000.pascal().toHuman(Pressure.kiloPascal) shouldBeEqualTo "100.0 kPa"
    }

    @Test
    fun `toHuman 자동 단위 선택이 올바르다`() {
        500.pascal().toHuman() shouldBeEqualTo "5.0 hPa"
        1500.pascal().toHuman() shouldBeEqualTo "1.5 kPa"
        100_000.pascal().toHuman() shouldBeEqualTo "1.0 bar"
    }
}
