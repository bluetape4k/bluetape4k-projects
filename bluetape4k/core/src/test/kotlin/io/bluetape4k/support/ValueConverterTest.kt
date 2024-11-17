package io.bluetape4k.support

import io.bluetape4k.junit5.params.provider.argumentOf
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.toBigDecimal

@RandomizedTest
class ValueConverterTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `convert any as char`() {
        val one = 'A'
        val nullValue: Char? = null

        one.asChar() shouldBeEqualTo 'A'
        nullValue.asChar() shouldBeEqualTo '\u0000'

        "".asChar() shouldBeEqualTo '\u0000'
        "C".asChar() shouldBeEqualTo 'C'
        "1".asChar() shouldBeEqualTo '1'
        "\t".asChar() shouldBeEqualTo '\t'

        "5000".asChar() shouldBeEqualTo 5000.toChar()

        log.trace { "5000.asChar() = ${5000.asChar()}" }
        5000.asChar() shouldBeEqualTo 5000.toChar()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asChar`(@RandomValue(type = Int::class, size = 100) expects: List<Int>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asChar() shouldBeEqualTo expected.toChar()
        }
    }

    @Test
    fun `convert any as byte`() {
        val one = "1".toByte()
        val nullValue: Byte? = null

        one.asByte() shouldBeEqualTo 1.toByte()
        nullValue.asByte() shouldBeEqualTo 0.toByte()

        "".asByte() shouldBeEqualTo 0.toByte()
        "C".asByte() shouldBeEqualTo 0.toByte()
        "1".asByte() shouldBeEqualTo 1.toByte()
        "\t".asByte() shouldBeEqualTo 0.toByte()

        12.asByte() shouldBeEqualTo 12.toByte()
        "12".asByte() shouldBeEqualTo 12.toByte()


        "5000".asByte() shouldBeEqualTo 5000.toByte()

        log.trace { "5000.asByte() = ${5000.asByte()}" }
        5000.asByte() shouldBeEqualTo 5000.toByte()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asByte`(@RandomValue(type = Int::class, size = 100) expects: List<Int>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asByte() shouldBeEqualTo expected.toByte()
        }
    }

    @Test
    fun `convert any as short`() {
        val one = "1".toShort()
        val nullValue: Short? = null

        one.asShort() shouldBeEqualTo 1.toShort()
        nullValue.asShort() shouldBeEqualTo 0.toShort()

        "".asShort() shouldBeEqualTo 0.toShort()
        "C".asShort() shouldBeEqualTo 0.toShort()
        "1".asShort() shouldBeEqualTo 1.toShort()
        "\t".asShort() shouldBeEqualTo 0.toShort()

        12.asShort() shouldBeEqualTo 12.toShort()
        "12".asShort() shouldBeEqualTo 12.toShort()


        "5000".asShort() shouldBeEqualTo 5000.toShort()

        log.trace { "5000.asShort() = ${5000.asShort()}" }
        5000.asShort() shouldBeEqualTo 5000.toShort()

        Short.MAX_VALUE.toString().asShort() shouldBeEqualTo Short.MAX_VALUE
        Short.MIN_VALUE.toString().asShort() shouldBeEqualTo Short.MIN_VALUE
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asShort`(@RandomValue(type = Int::class, size = 100) expects: List<Int>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asShort() shouldBeEqualTo expected.toShort()
        }
    }

    @Test
    fun `convert any as Int`() {
        val one = "1"
        val nullValue: Int? = null

        one.asInt() shouldBeEqualTo 1
        nullValue.asInt() shouldBeEqualTo 0

        "".asInt() shouldBeEqualTo 0
        "C".asInt() shouldBeEqualTo 0
        "1".asInt() shouldBeEqualTo 1
        "\t".asInt() shouldBeEqualTo 0

        12.asInt() shouldBeEqualTo 12
        "12".asInt() shouldBeEqualTo 12

        "5000".asInt() shouldBeEqualTo 5000

        log.trace { "5000.asInt() = ${5000.asInt()}" }
        5000.asInt() shouldBeEqualTo 5000
    }

    @Test
    fun `convert any as Int or Null`() {
        val one = "1"
        val nullValue: Int? = null

        one.asIntOrNull() shouldBeEqualTo 1
        nullValue.asIntOrNull().shouldBeNull()

        "".asIntOrNull().shouldBeNull()
        "C".asIntOrNull().shouldBeNull()
        "1".asIntOrNull() shouldBeEqualTo 1
        "\t".asIntOrNull().shouldBeNull()

        12.asIntOrNull() shouldBeEqualTo 12
        "12".asIntOrNull() shouldBeEqualTo 12

        "5000".asIntOrNull() shouldBeEqualTo 5000

        log.trace { "5000.asIntOrNull() = ${5000.asIntOrNull()}" }
        5000.asIntOrNull() shouldBeEqualTo 5000
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asInt`(@RandomValue(type = Long::class, size = 100) expects: List<Long>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asInt() shouldBeEqualTo expected.toInt()
        }
    }

    @Test
    fun `convert any as Long`() {
        val one = "1"
        val nullValue: Long? = null

        one.asLong() shouldBeEqualTo 1L
        nullValue.asLong() shouldBeEqualTo 0L

        "".asLong() shouldBeEqualTo 0L
        "C".asLong() shouldBeEqualTo 0L
        "1".asLong() shouldBeEqualTo 1L
        "\t".asLong() shouldBeEqualTo 0L

        12.asLong() shouldBeEqualTo 12L
        "12".asLong() shouldBeEqualTo 12L


        "5000".asLong() shouldBeEqualTo 5000L

        log.trace { "5000.asLong() = ${5000.asLong()}" }
        5000.asLong() shouldBeEqualTo 5000L
    }

    private fun getLongValues(): List<Arguments> = listOf(
        argumentOf("0", 0L),
        argumentOf("2", 2L),
        argumentOf(Long.MIN_VALUE.toString(), Long.MIN_VALUE),
        argumentOf(Long.MAX_VALUE.toString(), Long.MAX_VALUE),
        argumentOf("227366841360584705", 227366841360584705L),
        argumentOf("9223372036854775806", 9223372036854775806L)
    )

    @ParameterizedTest(name = "source={0}, expected={1}")
    @MethodSource("getLongValues")
    fun `convert any parameter asLong`(src: Any?, expected: Long) {
        src.asLong() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asLong`(@RandomValue(type = BigDecimal::class, size = 100) expects: List<BigDecimal>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asLong() shouldBeEqualTo expected.toLong()
        }
    }

    @Test
    fun `convert any as Float`() {
        val one = "1"
        val nullValue: Float? = null

        one.asFloat() shouldBeEqualTo 1.0F
        nullValue.asFloat() shouldBeEqualTo 0.0F

        "".asFloat() shouldBeEqualTo 0.0F
        "C".asFloat() shouldBeEqualTo 0.0F
        "1".asFloat() shouldBeEqualTo 1.0F
        "\t".asFloat() shouldBeEqualTo 0.0F

        12.asFloat() shouldBeEqualTo 12.0F
        "12".asFloat() shouldBeEqualTo 12.0F


        "5000".asFloat() shouldBeEqualTo 5000.0F

        log.trace { "5000.asFloat() = ${5000.asFloat()}" }
        5000.asFloat() shouldBeEqualTo 5000.0F
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asFloat`(@RandomValue(type = Double::class, size = 100) expects: List<Double>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asFloat() shouldBeEqualTo expected.toFloat()
        }
    }

    @Test
    fun `convert any as Double`() {
        val one = "1"
        val nullValue: Double? = null

        one.asDouble() shouldBeEqualTo 1.0
        nullValue.asDouble() shouldBeEqualTo 0.0

        "".asDouble() shouldBeEqualTo 0.0
        "C".asDouble() shouldBeEqualTo 0.0
        "1".asDouble() shouldBeEqualTo 1.0
        "\t".asDouble() shouldBeEqualTo 0.0

        12.asDouble() shouldBeEqualTo 12.0
        "12".asDouble() shouldBeEqualTo 12.0


        "5000".asDouble() shouldBeEqualTo 5000.0

        log.trace { "5000.asDouble() = ${5000.asDouble()}" }
        5000.asDouble() shouldBeEqualTo 5000.0
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asDouble`(@RandomValue(type = BigDecimal::class, size = 100) expects: List<BigDecimal>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asDouble() shouldBeEqualTo expected.toDouble()
        }
    }

    @Test
    fun `convert any as BigInteger`() {
        val one = "1"
        val nullValue: Byte? = null

        one.asBigInt() shouldBeEqualTo BigInteger.ONE
        nullValue.asBigInt() shouldBeEqualTo BigInteger.ZERO

        "".asBigInt() shouldBeEqualTo BigInteger.ZERO
        "C".asBigInt() shouldBeEqualTo BigInteger.ZERO
        "1".asBigInt() shouldBeEqualTo BigInteger.ONE
        "\t".asBigInt() shouldBeEqualTo BigInteger.ZERO

        12.asBigInt() shouldBeEqualTo 12.toBigInteger()
        "12".asBigInt() shouldBeEqualTo 12.toBigInteger()


        "5000".asBigInt() shouldBeEqualTo 5000.toBigInteger()

        log.trace { "5000.asBigInt() = ${5000.asBigInt()}" }
        5000.asBigInt() shouldBeEqualTo 5000.toBigInteger()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigInt`(@RandomValue(type = BigInteger::class, size = 100) expects: List<BigInteger>) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asBigInt() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any as BigDecimal`() {
        val one = "1"
        val nullValue: Byte? = null

        one.asBigDecimal() shouldBeEqualTo BigDecimal.ONE
        nullValue.asBigDecimal() shouldBeEqualTo BigDecimal.ZERO

        "".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "C".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "1".asBigDecimal() shouldBeEqualTo BigDecimal.ONE
        "\t".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO

        12.asBigDecimal() shouldBeEqualTo 12.toBigDecimal()
        "12".asBigDecimal() shouldBeEqualTo 12.toBigDecimal()


        "5000".asBigDecimal() shouldBeEqualTo 5000.toBigDecimal()

        log.trace { "5000.asBigDecimal() = ${5000.asBigDecimal()}" }
        5000.asBigDecimal() shouldBeEqualTo 5000.toBigDecimal()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigDecimal`(
        @RandomValue(
            type = BigDecimal::class, size = 100
        ) expects: List<BigDecimal>,
    ) {
        expects.forEach { expected ->
            val target = expected.toString()
            target.asBigDecimal() shouldBeEqualTo expected
        }
    }

    @Test
    fun `floor float number`() {
        val one = 1.0012345f
        val one1 = 1.011111f
        val one5 = 1.050234f
        val one49 = 1.049999f
        val empty: Float? = null

        one.asFloatFloor(2) shouldBeEqualTo 1.00F
        one.asFloatFloor(1) shouldBeEqualTo 1.0F

        one1.asFloatFloor(2) shouldBeEqualTo 1.01F
        one1.asFloatFloor(1) shouldBeEqualTo 1.0F

        one5.asFloatFloor(2) shouldBeEqualTo 1.05F
        one5.asFloatFloor(1) shouldBeEqualTo 1.0F

        one49.asFloatFloor(2) shouldBeEqualTo 1.04F
        one49.asFloatFloor(1) shouldBeEqualTo 1.0F

        empty.asFloatFloor(2) shouldBeEqualTo 0.00F
        empty.asFloatFloor(1) shouldBeEqualTo 0.0F
    }

    @Test
    fun `floor double number`() {
        val one = 1.00123456
        val one1 = 1.011111
        val one5 = 1.0512341
        val one49 = 1.0499999999
        val empty: Double? = null

        one.asDoubleFloor(2) shouldBeEqualTo 1.00
        one.asDoubleFloor(1) shouldBeEqualTo 1.0

        one1.asDoubleFloor(2) shouldBeEqualTo 1.01
        one1.asDoubleFloor(1) shouldBeEqualTo 1.0

        one5.asDoubleFloor(2) shouldBeEqualTo 1.05
        one5.asDoubleFloor(1) shouldBeEqualTo 1.0

        one49.asDoubleFloor(2) shouldBeEqualTo 1.04
        one49.asDoubleFloor(1) shouldBeEqualTo 1.0

        empty.asDoubleFloor(2) shouldBeEqualTo 0.00
        empty.asDoubleFloor(1) shouldBeEqualTo 0.0


        "13567.6".asDoubleFloor(-2) shouldBeEqualTo 13500.0
    }

    @Test
    fun `round float number`() {
        val one = 1.0012345f
        val one1 = 1.011111f
        val one5 = 1.050234f
        val one49 = 1.049999f
        val empty: Float? = uninitialized()

        one.asFloatRound(2) shouldBeEqualTo 1.00F
        one.asFloatRound(1) shouldBeEqualTo 1.0F

        one1.asFloatRound(2) shouldBeEqualTo 1.01F
        one1.asFloatRound(1) shouldBeEqualTo 1.0F

        one5.asFloatRound(2) shouldBeEqualTo 1.05F
        one5.asFloatRound(1) shouldBeEqualTo 1.1F

        one49.asFloatRound(2) shouldBeEqualTo 1.05F
        one49.asFloatRound(1) shouldBeEqualTo 1.0F

        empty.asFloatRound(2) shouldBeEqualTo 0.00F
        empty.asFloatRound(1) shouldBeEqualTo 0.0F
    }

    @Test
    fun `round double number`() {
        val one = 1.00123456
        val one1 = 1.011111
        val one5 = 1.0512341
        val one49 = 1.0499999999
        val empty: Double? = uninitialized()

        one.asDoubleRound(2) shouldBeEqualTo 1.00
        one.asDoubleRound(1) shouldBeEqualTo 1.0

        one1.asDoubleRound(2) shouldBeEqualTo 1.01
        one1.asDoubleRound(1) shouldBeEqualTo 1.0

        one5.asDoubleRound(2) shouldBeEqualTo 1.05
        one5.asDoubleRound(1) shouldBeEqualTo 1.1

        one49.asDoubleRound(2) shouldBeEqualTo 1.05
        one49.asDoubleRound(1) shouldBeEqualTo 1.0

        empty.asDoubleRound(2) shouldBeEqualTo 0.00
        empty.asDoubleRound(1) shouldBeEqualTo 0.0
    }
}
