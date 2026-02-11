package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import kotlin.test.assertFailsWith
import kotlin.toBigDecimal

class NumberSupportTest {

    companion object: KLogging()

    @Test
    fun `coerceIn operator`() {
        32.coerceIn(38, 42) shouldBeEqualTo 38
        32.coerceIn(38..42) shouldBeEqualTo 38

        44.coerceIn(38, 42) shouldBeEqualTo 42
        44.coerceIn(38..42) shouldBeEqualTo 42

        40.coerceIn(38, 42) shouldBeEqualTo 40
        40.coerceIn(38..42) shouldBeEqualTo 40
    }

    @Test
    fun `toHuman으로 숫자를 사람이 읽기 쉬운 문자열로 변환한다`() {
        1234567890.toHuman() shouldContain "1,234,567,890"
        0.toHuman() shouldBeEqualTo "0"
        1234.5.toHuman() shouldContain "1,234.5"

        3.14192.toHuman() shouldContain "3.1"
        3.14192.toHuman("#.##") shouldContain "3.14"
    }

    @Test
    fun `string is hex number`() {
        "0x74".isHexFormat().shouldBeTrue()
        "0XFF".isHexFormat().shouldBeTrue()
        "#3A".isHexFormat().shouldBeTrue()
        "-0xAD".isHexFormat().shouldBeTrue()
        "0X1234".isHexFormat().shouldBeTrue()
        "0x1234".isHexFormat().shouldBeTrue()

        // 유효하지 않은 hex 포맷
        "0X123h".isHexFormat().shouldBeFalse()
        "X0FF".isHexFormat().shouldBeFalse()
        "0xZZ".isHexFormat().shouldBeFalse()

        // 일반 10진수, 빈 문자열, prefix만 있는 경우
        "1234".isHexFormat().shouldBeFalse()
        "".isHexFormat().shouldBeFalse()
        " ".isHexFormat().shouldBeFalse()
        "0x".isHexFormat().shouldBeFalse()
        "#".isHexFormat().shouldBeFalse()
    }

    @Test
    fun `decode string to BigInteger`() {
        "".decodeBigInt() shouldBeEqualTo BigInteger.ZERO
        " ".decodeBigInt() shouldBeEqualTo BigInteger.ZERO
        "-1".decodeBigInt() shouldBeEqualTo BigInteger.ONE.negate()
        "#42".decodeBigInt() shouldBeEqualTo BigInteger.valueOf(0x42L)
        "0x42".decodeBigInt() shouldBeEqualTo BigInteger.valueOf(0x42L)
        "-0x42".decodeBigInt() shouldBeEqualTo BigInteger.valueOf(-0x42L)
        "077".decodeBigInt() shouldBeEqualTo BigInteger.valueOf(63L) // 8진수
    }

    @Test
    fun `decode string to BigDecimal`() {
        "".decodeBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "-1".decodeBigDecimal() shouldBeEqualTo BigDecimal.ONE.negate()
        "1.0".decodeBigDecimal() shouldBeEqualTo BigDecimal.ONE

        assertFailsWith<NumberFormatException> {
            "#42".decodeBigDecimal()
        }
        assertFailsWith<NumberFormatException> {
            "0x42".decodeBigDecimal()
        }
    }

    @Test
    fun `parse string to number`() {
        "0x42".parseNumber<Int>() shouldBeEqualTo 0x42
        "-0x42".parseNumber<Int>() shouldBeEqualTo -0x42

        " 42 ".parseNumber<Byte>() shouldBeEqualTo 42.toByte()
        " 42 ".parseNumber<Short>() shouldBeEqualTo 42.toShort()
        " 42 ".parseNumber<Int>() shouldBeEqualTo 42
        " 42 ".parseNumber<Long>() shouldBeEqualTo 42L

        "42.4".parseNumber<Float>() shouldBeEqualTo 42.4F
        "42.4".parseNumber<Double>() shouldBeEqualTo 42.4

        "42".parseNumber<BigInteger>() shouldBeEqualTo 42.toBigInt()
        "42.4".parseNumber<BigDecimal>() shouldBeEqualTo 42.4.toBigDecimal()

        // hex 문자열을 BigInteger, BigDecimal로 파싱
        "0x42".parseNumber<BigInteger>() shouldBeEqualTo BigInteger.valueOf(0x42L)
        "0x42".parseNumber<BigDecimal>() shouldBeEqualTo BigDecimal.valueOf(0x42L)
    }

    @Test
    fun `parse string to number with NumberFormat`() {
        val format = DecimalFormat("#,##0.#")

        "1,234".parseNumber<Int>(format) shouldBeEqualTo 1234
        "1,234".parseNumber<Long>(format) shouldBeEqualTo 1234L
        "1,234.5".parseNumber<Double>(format) shouldBeEqualTo 1234.5
        "1,234.5".parseNumber<Float>(format) shouldBeEqualTo 1234.5F
        "1,234.5".parseNumber<BigDecimal>(format).compareTo(BigDecimal("1234.5")) shouldBeEqualTo 0
    }

    @Test
    fun `parse invalid number format string`() {
        assertFailsWith<NumberFormatException> {
            "42.4".parseNumber<Byte>()
        }
        assertFailsWith<NumberFormatException> {
            "42.4".parseNumber<Int>()
        }
        assertFailsWith<NumberFormatException> {
            "42.4".parseNumber<Long>()
        }
        assertFailsWith<NumberFormatException> {
            "42L".parseNumber<Long>()
        }
        assertFailsWith<NumberFormatException> {
            "42.4".parseNumber<BigInteger>()
        }
    }

    @Test
    fun `toTargetClass로 숫자 타입을 변환한다`() {
        // 동일 타입 반환
        42.toTargetClass<Int>() shouldBeEqualTo 42

        // 타입 간 변환
        42.toTargetClass<Long>() shouldBeEqualTo 42L
        42.toTargetClass<Short>() shouldBeEqualTo 42.toShort()
        42.toTargetClass<Byte>() shouldBeEqualTo 42.toByte()
        42.toTargetClass<Float>() shouldBeEqualTo 42.0F
        42.toTargetClass<Double>() shouldBeEqualTo 42.0
        42.toTargetClass<BigInteger>() shouldBeEqualTo 42.toBigInt()
        42.toTargetClass<BigDecimal>() shouldBeEqualTo 42.toBigDecimal()

        // Float/Double → BigInteger 변환 (오버플로우 없이 동작)
        1.0E18.toTargetClass<BigInteger>() shouldBeEqualTo BigDecimal("1.0E18").toBigInteger()
        123.45F.toTargetClass<BigInteger>() shouldBeEqualTo 123.45F.toBigDecimal().toBigInteger()
    }

    @Test
    fun `toTargetClass 오버플로우 시 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            Long.MAX_VALUE.toTargetClass<Int>()
        }
        assertFailsWith<IllegalArgumentException> {
            256.toTargetClass<Byte>()
        }
        assertFailsWith<IllegalArgumentException> {
            100_000.toTargetClass<Short>()
        }
    }
}
