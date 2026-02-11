package io.bluetape4k.support

import io.bluetape4k.junit5.params.provider.argumentOf
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.toBigDecimal

@RandomizedTest
class ValueConvertersTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `convert any to boolean`() {
        null.asBooleanOrNull().shouldBeNull()
        null.asBoolean(true).shouldBeTrue()
        null.asBoolean(false).shouldBeFalse()

        true.asBoolean().shouldBeTrue()
        "true".asBoolean().shouldBeTrue()
        "TRUE".asBoolean().shouldBeTrue()

        false.asBoolean().shouldBeFalse()
        "false".asBoolean().shouldBeFalse()
        "FALSE".asBoolean().shouldBeFalse()

        0.asBoolean().shouldBeFalse()
        1.asBoolean().shouldBeTrue()

        'Y'.asBoolean().shouldBeTrue()
        'y'.asBoolean().shouldBeTrue()

        'N'.asBoolean().shouldBeFalse()
        'n'.asBoolean().shouldBeFalse()

        "OK".asBoolean().shouldBeFalse()
    }

    @Test
    fun `convert any to char`() {
        val one = 'A'
        val nullValue: Any? = null

        one.asChar() shouldBeEqualTo 'A'
        nullValue.asChar() shouldBeEqualTo ZERO_CHAR

        "".asChar() shouldBeEqualTo ZERO_CHAR
        "C".asChar() shouldBeEqualTo 'C'
        "1".asChar() shouldBeEqualTo '1'
        "\t".asChar() shouldBeEqualTo '\t'

        // 127 을 넘으면 Unicode 문자가 됩니다.
        log.debug { "5000.asCharOrNull() = ${5000.asCharOrNull()}" }
        "5000".asChar() shouldBeEqualTo 5000.toChar()
        5000.asChar() shouldBeEqualTo 5000.toChar()

        3.14.asCharOrNull() shouldBeEqualTo 3.toChar()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random int to char`(@RandomValue(type = Char::class, size = 100) expects: List<Char>) {
        expects.forEach { expected ->
            expected.toString().asChar() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to byte`() {
        val one = 1.toByte()
        val nullValue: Byte? = null

        one.asByte() shouldBeEqualTo 1.toByte()
        nullValue.asByte() shouldBeEqualTo ZERO_BYTE

        "".asByte() shouldBeEqualTo ZERO_BYTE
        "C".asByte() shouldBeEqualTo ZERO_BYTE
        "\t".asByte() shouldBeEqualTo ZERO_BYTE

        1.asByte() shouldBeEqualTo 1.toByte()
        "1".asByte() shouldBeEqualTo 1.toByte()

        12.asByte() shouldBeEqualTo 12.toByte()
        "12".asByte() shouldBeEqualTo 12.toByte()

        log.debug { "5000.asByteOrNull() = ${5000.asByteOrNull()}" }
        "5000".asByte() shouldBeEqualTo ZERO_BYTE
        5000.asByteOrNull() shouldBeEqualTo 5000.toByte()

        3.14.asByteOrNull() shouldBeEqualTo 3.toByte()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asByte`(@RandomValue(type = Byte::class, size = 100) expects: List<Byte>) {
        expects.forEach { expected ->
            expected.toString().asByte() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to short`() {
        val one = 1.toShort()
        val nullValue: Short? = null

        one.asShort() shouldBeEqualTo 1.toShort()
        nullValue.asShort() shouldBeEqualTo 0.toShort()

        "".asShort() shouldBeEqualTo 0.toShort()
        "C".asShort() shouldBeEqualTo 0.toShort()
        "\t".asShort() shouldBeEqualTo 0.toShort()

        1.asShort() shouldBeEqualTo 1.toShort()
        "1".asShort() shouldBeEqualTo 1.toShort()
        '1'.asShort() shouldBeEqualTo '1'.code.toShort()

        12.asShort() shouldBeEqualTo 12.toShort()
        "12".asShort() shouldBeEqualTo 12.toShort()

        log.debug { "5000.asShortOrNull() = ${5000.asShortOrNull()}" }
        "5000".asShort() shouldBeEqualTo 5000.toShort()
        5000.asShort() shouldBeEqualTo 5000.toShort()

        Short.MAX_VALUE.toString().asShort() shouldBeEqualTo Short.MAX_VALUE
        Short.MIN_VALUE.toString().asShort() shouldBeEqualTo Short.MIN_VALUE

        3.14.asShortOrNull() shouldBeEqualTo 3.toShort()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value to short`(@RandomValue(type = Short::class, size = 100) expects: List<Short>) {
        expects.forEach { expected ->
            expected.toString().asShort() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to Int`() {
        val one = "1"
        val nullValue: Int? = null

        one.asInt() shouldBeEqualTo 1
        nullValue.asInt() shouldBeEqualTo 0

        "".asInt() shouldBeEqualTo 0
        "C".asInt() shouldBeEqualTo 0
        "\t".asInt() shouldBeEqualTo 0

        1.asInt() shouldBeEqualTo 1
        "1".asInt() shouldBeEqualTo 1

        12.asInt() shouldBeEqualTo 12
        "12".asInt() shouldBeEqualTo 12

        log.debug { "5000.asInt() = ${5000.asIntOrNull()}" }
        "5000".asInt() shouldBeEqualTo 5000
        5000.asInt() shouldBeEqualTo 5000

        Int.MAX_VALUE.toString().asInt() shouldBeEqualTo Int.MAX_VALUE
        Int.MIN_VALUE.toString().asInt() shouldBeEqualTo Int.MIN_VALUE

        3.14.asIntOrNull() shouldBeEqualTo 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asInt`(@RandomValue(type = Int::class, size = 100) expects: List<Int>) {
        expects.forEach { expected ->
            expected.toString().asInt() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to Long`() {
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

        log.debug { "5000.asLong() = ${5000.asLong()}" }
        "5000".asLong() shouldBeEqualTo 5000L
        5000.asLong() shouldBeEqualTo 5000L

        Long.MAX_VALUE.toString().asLong() shouldBeEqualTo Long.MAX_VALUE
        Long.MIN_VALUE.toString().asLong() shouldBeEqualTo Long.MIN_VALUE

        3.14.asLongOrNull() shouldBeEqualTo 3L
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
    fun `convert random value to Long`(@RandomValue(type = Long::class, size = 100) expects: List<Long>) {
        expects.forEach { expected ->
            expected.toString().asLong() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to Float`() {
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

        log.debug { "5000.asFloat() = ${5000.asFloat()}" }
        "5000".asFloat() shouldBeEqualTo 5000.0F
        5000.asFloat() shouldBeEqualTo 5000.0F

        Float.MAX_VALUE.toString().asFloat() shouldBeEqualTo Float.MAX_VALUE
        Float.MIN_VALUE.toString().asFloat() shouldBeEqualTo Float.MIN_VALUE
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value to float`(@RandomValue(type = Float::class, size = 100) expects: List<Float>) {
        expects.forEach { expected ->
            expected.toString().asFloat() shouldBeEqualTo expected
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

        log.debug { "5000.asDouble() = ${5000.asDouble()}" }
        "5000".asDouble() shouldBeEqualTo 5000.0
        5000.asDouble() shouldBeEqualTo 5000.0

        Double.MAX_VALUE.toString().asDouble() shouldBeEqualTo Double.MAX_VALUE
        Double.MIN_VALUE.toString().asDouble() shouldBeEqualTo Double.MIN_VALUE
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asDouble`(@RandomValue(type = Double::class, size = 100) expects: List<Double>) {
        expects.forEach { expected ->
            expected.toString().asDouble() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any as BigInteger`() {
        val one = "1"
        val nullValue: BigInteger? = null

        one.asBigInt() shouldBeEqualTo BigInteger.ONE
        nullValue.asBigInt() shouldBeEqualTo BigInteger.ZERO

        "".asBigInt() shouldBeEqualTo BigInteger.ZERO
        "C".asBigInt() shouldBeEqualTo BigInteger.ZERO
        "1".asBigInt() shouldBeEqualTo BigInteger.ONE
        "\t".asBigInt() shouldBeEqualTo BigInteger.ZERO

        12.asBigInt() shouldBeEqualTo 12.toBigInteger()
        "12".asBigInt() shouldBeEqualTo 12.toBigInteger()

        log.debug { "5000.asBigInt() = ${5000.asBigInt()}" }
        "5000".asBigInt() shouldBeEqualTo 5000.toBigInteger()
        5000.asBigInt() shouldBeEqualTo 5000.toBigInteger()

        Long.MAX_VALUE.toString().asBigInt() shouldBeEqualTo Long.MAX_VALUE.toBigInt()
        Long.MIN_VALUE.toString().asBigInt() shouldBeEqualTo Long.MIN_VALUE.toBigInt()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigInt`(@RandomValue(type = BigInteger::class, size = 100) expects: List<BigInteger>) {
        expects.forEach { expected ->
            expected.toString().asBigInt() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any as BigDecimal`() {
        val one = "1"
        val nullValue: BigDecimal? = null

        one.asBigDecimal() shouldBeEqualTo BigDecimal.ONE
        nullValue.asBigDecimal() shouldBeEqualTo BigDecimal.ZERO

        "".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "C".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "1".asBigDecimal() shouldBeEqualTo BigDecimal.ONE
        "\t".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO

        12.asBigDecimal() shouldBeEqualTo 12.toBigDecimal()
        "12".asBigDecimal() shouldBeEqualTo 12.toBigDecimal()

        log.debug { "5000.asBigDecimal() = ${5000.asBigDecimal()}" }
        "5000".asBigDecimal() shouldBeEqualTo 5000.toBigDecimal()
        5000.asBigDecimal() shouldBeEqualTo 5000.toBigDecimal()

        Double.MAX_VALUE.asBigDecimal() shouldBeEqualTo Double.MAX_VALUE.toBigDecimal()
        Double.MIN_VALUE.asBigDecimal() shouldBeEqualTo Double.MIN_VALUE.toBigDecimal()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigDecimal`(
        @RandomValue(
            type = BigDecimal::class,
            size = 100
        ) expects: List<BigDecimal>,
    ) {
        expects.forEach { expected ->
            expected.toString().asBigDecimal() shouldBeEqualTo expected
        }
    }

    @Test
    fun `convert any to String`() {
        val one = "1"
        val nullValue: String? = null

        one.asString() shouldBeEqualTo one
        nullValue.asString() shouldBeEqualTo EMPTY_STRING

        "".asString() shouldBeEqualTo EMPTY_STRING
        "C".asString() shouldBeEqualTo "C"
        "1".asString() shouldBeEqualTo "1"
        "\t".asString() shouldBeEqualTo "\t"

        'C'.asString() shouldBeEqualTo "C"

        12.asString() shouldBeEqualTo "12"
        5000.asString() shouldBeEqualTo "5000"

        Double.MAX_VALUE.asString() shouldBeEqualTo Double.MAX_VALUE.toString()
        Double.MIN_VALUE.asString() shouldBeEqualTo Double.MIN_VALUE.toString()
    }

    @Test
    fun `convert any to Date`() {
        val nullValue: Date? = null


        nullValue.asDateOrNull().shouldBeNull()
        nullValue.asDate() shouldBeEqualTo Date(0L)

        val timestamp = System.currentTimeMillis()
        val today = Date(timestamp)

        timestamp.asDate() shouldBeEqualTo Date(timestamp)
        today.asDate() shouldBeEqualTo today
    }

    @Test
    fun `convert any to Timestamp`() {
        val nullValue: Timestamp? = null

        nullValue.asTimestampOrNull().shouldBeNull()
        nullValue.asTimestamp() shouldBeEqualTo Timestamp(0L)

        val epochMills = System.currentTimeMillis()
        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asTimestamp() shouldBeEqualTo timestamp
        today.asTimestamp() shouldBeEqualTo timestamp

        Instant.ofEpochMilli(epochMills).asTimestamp() shouldBeEqualTo timestamp
        epochMills.asTimestamp() shouldBeEqualTo timestamp
    }

    @Test
    fun `convert any to Instant`() {
        val nullValue: Instant? = null

        nullValue.asInstantOrNull().shouldBeNull()
        nullValue.asInstant() shouldBeEqualTo Instant.ofEpochMilli(0)

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        epochMills.asInstant() shouldBeEqualTo now
        now.asInstant() shouldBeEqualTo now

        timestamp.asInstant() shouldBeEqualTo now
        today.asInstant() shouldBeEqualTo now

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
    }

    @Test
    fun `convert any to LocalDate`() {
        val nullValue: LocalDate? = null

        nullValue.asLocalDateOrNull().shouldBeNull()
        nullValue.asLocalDate() shouldBeEqualTo LocalDate.MIN

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val expected = LocalDate.ofInstant(now, ZoneOffset.UTC)

        now.asLocalDate() shouldBeEqualTo expected
        epochMills.asLocalDate() shouldBeEqualTo expected

        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asLocalDate() shouldBeEqualTo expected
        today.asLocalDate() shouldBeEqualTo expected

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDate() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDate() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDate() shouldBeEqualTo expected
    }

    @Test
    fun `convert any to LocalTime`() {
        val nullValue: LocalTime? = null

        nullValue.asLocalTimeOrNull().shouldBeNull()
        nullValue.asLocalTime() shouldBeEqualTo LocalTime.MIN

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val expected = LocalTime.ofInstant(now, ZoneOffset.UTC)

        now.asLocalTime() shouldBeEqualTo expected
        epochMills.asLocalTime() shouldBeEqualTo expected

        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asLocalTime() shouldBeEqualTo expected
        today.asLocalTime() shouldBeEqualTo expected

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asLocalTime() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalTime() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asLocalTime() shouldBeEqualTo expected
    }

    @Test
    fun `convert any to LocalDateTime`() {
        val nullValue: LocalDateTime? = null

        nullValue.asLocalDateTimeOrNull().shouldBeNull()
        nullValue.asLocalDateTime() shouldBeEqualTo LocalDateTime.MIN

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val expected = LocalDateTime.ofInstant(now, ZoneOffset.UTC)

        now.asLocalDateTime() shouldBeEqualTo expected
        epochMills.asLocalDateTime() shouldBeEqualTo expected

        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asLocalDateTime() shouldBeEqualTo expected
        today.asLocalDateTime() shouldBeEqualTo expected

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDateTime() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDateTime() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDateTime() shouldBeEqualTo expected
    }


    @Test
    fun `convert any to OffsetDateTime`() {
        val nullValue: OffsetDateTime? = null

        nullValue.asOffsetDateTimeOrNull().shouldBeNull()
        nullValue.asOffsetDateTime() shouldBeEqualTo OffsetDateTime.MIN

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val expected = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)

        now.asOffsetDateTime() shouldBeEqualTo expected
        epochMills.asOffsetDateTime() shouldBeEqualTo expected

        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asOffsetDateTime() shouldBeEqualTo expected
        today.asOffsetDateTime() shouldBeEqualTo expected

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asOffsetDateTime() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asOffsetDateTime() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asOffsetDateTime() shouldBeEqualTo expected
    }

    @Test
    fun `convert any to ZonedDateTime`() {
        val nullValue: ZonedDateTime? = null

        nullValue.asZonedDateTimeOrNull().shouldBeNull()
        nullValue.asZonedDateTime() shouldBeEqualTo ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)

        val epochMills = System.currentTimeMillis()
        val now = Instant.ofEpochMilli(epochMills)
        val expected = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)

        now.asZonedDateTime() shouldBeEqualTo expected
        epochMills.asZonedDateTime() shouldBeEqualTo expected

        val timestamp = Timestamp(epochMills)
        val today = Date(timestamp.time)

        timestamp.asZonedDateTime() shouldBeEqualTo expected
        today.asZonedDateTime() shouldBeEqualTo expected

        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
    }

    @Test
    fun `convert any to UUID`() {
        val nullValue: UUID? = null

        nullValue.asUUIDOrNull().shouldBeNull()
        nullValue.asUUID() shouldBeEqualTo ZERO_UUID

        val uuidStr = "24738134-9d88-6645-4ec8-d63aa2031015"
        val uuid = UUID.fromString(uuidStr)
        uuidStr.asUUID() shouldBeEqualTo uuid

        val bigInt = uuid.toBigInt()
        bigInt.asUUID() shouldBeEqualTo uuid
    }

    @Test
    fun `convert any to byte array`() {
        val nullValue: ByteArray? = null

        nullValue.asByteArrayOrNull().shouldBeNull()
        nullValue.asByteArray() shouldBeEqualTo emptyByteArray

        val array = byteArrayOf(1, 2, 3)
        array.asByteArray() shouldBeEqualTo array

        val str = "동해물과 백두산이"
        str.asByteArray() shouldBeEqualTo str.toUtf8Bytes()

        42.asByteArray() shouldBeEqualTo 42.toByteArray()
        42L.asByteArray() shouldBeEqualTo 42L.toByteArray()

        val uuid = UUID.randomUUID()
        uuid.asByteArray() shouldBeEqualTo uuid.toByteArray()
    }

    @Test
    fun `floor float number`() {
        val one = 1.0012345f
        val one1 = 1.011111f
        val one5 = 1.050234f
        val one49 = 1.049999f
        val nullValue: Float? = null

        one.asFloatFloor(2) shouldBeEqualTo 1.00F
        one.asFloatFloor(1) shouldBeEqualTo 1.0F

        one1.asFloatFloor(2) shouldBeEqualTo 1.01F
        one1.asFloatFloor(1) shouldBeEqualTo 1.0F

        one5.asFloatFloor(2) shouldBeEqualTo 1.05F
        one5.asFloatFloor(1) shouldBeEqualTo 1.0F

        one49.asFloatFloor(2) shouldBeEqualTo 1.04F
        one49.asFloatFloor(1) shouldBeEqualTo 1.0F

        nullValue.asFloatFloor(2) shouldBeEqualTo 0.00F
        nullValue.asFloatFloor(1) shouldBeEqualTo 0.0F
    }

    @Test
    fun `floor double number`() {
        val one = 1.00123456
        val one1 = 1.011111
        val one5 = 1.0512341
        val one49 = 1.0499999999
        val nullValue: Double? = null

        one.asDoubleFloor(2) shouldBeEqualTo 1.00
        one.asDoubleFloor(1) shouldBeEqualTo 1.0

        one1.asDoubleFloor(2) shouldBeEqualTo 1.01
        one1.asDoubleFloor(1) shouldBeEqualTo 1.0

        one5.asDoubleFloor(2) shouldBeEqualTo 1.05
        one5.asDoubleFloor(1) shouldBeEqualTo 1.0

        one49.asDoubleFloor(2) shouldBeEqualTo 1.04
        one49.asDoubleFloor(1) shouldBeEqualTo 1.0

        nullValue.asDoubleFloor(2) shouldBeEqualTo 0.00
        nullValue.asDoubleFloor(1) shouldBeEqualTo 0.0


        "13567.6".asDoubleFloor(-2) shouldBeEqualTo 13500.0
    }

    @Test
    fun `round float number`() {
        val one = 1.0012345f
        val one1 = 1.011111f
        val one5 = 1.050234f
        val one49 = 1.049999f
        val nullValue: Float? = null

        one.asFloatRound(2) shouldBeEqualTo 1.00F
        one.asFloatRound(1) shouldBeEqualTo 1.0F

        one1.asFloatRound(2) shouldBeEqualTo 1.01F
        one1.asFloatRound(1) shouldBeEqualTo 1.0F

        one5.asFloatRound(2) shouldBeEqualTo 1.05F
        one5.asFloatRound(1) shouldBeEqualTo 1.1F

        one49.asFloatRound(2) shouldBeEqualTo 1.05F
        one49.asFloatRound(1) shouldBeEqualTo 1.0F

        nullValue.asFloatRound(2) shouldBeEqualTo 0.00F
        nullValue.asFloatRound(1) shouldBeEqualTo 0.0F
    }

    @Test
    fun `round double number`() {
        val one = 1.00123456
        val one1 = 1.011111
        val one5 = 1.0512341
        val one49 = 1.0499999999
        val nullValue: Double? = null

        one.asDoubleRound(2) shouldBeEqualTo 1.00
        one.asDoubleRound(1) shouldBeEqualTo 1.0

        one1.asDoubleRound(2) shouldBeEqualTo 1.01
        one1.asDoubleRound(1) shouldBeEqualTo 1.0

        one5.asDoubleRound(2) shouldBeEqualTo 1.05
        one5.asDoubleRound(1) shouldBeEqualTo 1.1

        one49.asDoubleRound(2) shouldBeEqualTo 1.05
        one49.asDoubleRound(1) shouldBeEqualTo 1.0

        nullValue.asDoubleRound(2) shouldBeEqualTo 0.00
        nullValue.asDoubleRound(1) shouldBeEqualTo 0.0
    }

    @Test
    fun `ceil float number`() {
        val one = 1.0012345f
        val one1 = 1.011111f
        val one5 = 1.050234f
        val one49 = 1.049999f
        val nullValue: Float? = null

        one.asFloatCeil(2) shouldBeEqualTo 1.01F
        one.asFloatCeil(1) shouldBeEqualTo 1.1F

        one1.asFloatCeil(2) shouldBeEqualTo 1.02F
        one1.asFloatCeil(1) shouldBeEqualTo 1.1F

        one5.asFloatCeil(2) shouldBeEqualTo 1.06F
        one5.asFloatCeil(1) shouldBeEqualTo 1.1F

        one49.asFloatCeil(2) shouldBeEqualTo 1.05F
        one49.asFloatCeil(1) shouldBeEqualTo 1.1F

        nullValue.asFloatCeil(2) shouldBeEqualTo 0.00F
        nullValue.asFloatCeil(1) shouldBeEqualTo 0.0F
    }

    @Test
    fun `ceil double number`() {
        val one = 1.00123456
        val one1 = 1.011111
        val one5 = 1.0512341
        val one49 = 1.0499999999
        val nullValue: Double? = null

        one.asDoubleCeil(2) shouldBeEqualTo 1.01
        one.asDoubleCeil(1) shouldBeEqualTo 1.1

        one1.asDoubleCeil(2) shouldBeEqualTo 1.02
        one1.asDoubleCeil(1) shouldBeEqualTo 1.1

        one5.asDoubleCeil(2) shouldBeEqualTo 1.06
        one5.asDoubleCeil(1) shouldBeEqualTo 1.1

        one49.asDoubleCeil(2) shouldBeEqualTo 1.05
        one49.asDoubleCeil(1) shouldBeEqualTo 1.1

        nullValue.asDoubleCeil(2) shouldBeEqualTo 0.00
        nullValue.asDoubleCeil(1) shouldBeEqualTo 0.0
    }
}
