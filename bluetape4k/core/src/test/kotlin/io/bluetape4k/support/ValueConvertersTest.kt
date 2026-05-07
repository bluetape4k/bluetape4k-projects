package io.bluetape4k.support

import io.bluetape4k.junit5.params.provider.argumentOf
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
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
        null.asBoolean(true).shouldBeTrue(); null.asBoolean(false).shouldBeFalse()
        true.asBoolean().shouldBeTrue(); "true".asBoolean().shouldBeTrue(); "TRUE".asBoolean().shouldBeTrue()
        false.asBoolean().shouldBeFalse(); "false".asBoolean().shouldBeFalse(); "FALSE".asBoolean().shouldBeFalse()
        0.asBoolean().shouldBeFalse(); 1.asBoolean().shouldBeTrue()
        'Y'.asBoolean().shouldBeTrue(); 'y'.asBoolean().shouldBeTrue()
        'N'.asBoolean().shouldBeFalse(); 'n'.asBoolean().shouldBeFalse()
        "OK".asBoolean().shouldBeFalse()
    }

    @Test
    fun `convert any to char`() {
        val nullValue: Any? = null
        'A'.asChar() shouldBeEqualTo 'A'; nullValue.asChar() shouldBeEqualTo ZERO_CHAR
        "".asChar() shouldBeEqualTo ZERO_CHAR; "C".asChar() shouldBeEqualTo 'C'; "1".asChar() shouldBeEqualTo '1'
        "\t".asChar() shouldBeEqualTo '\t'
        log.debug { "5000.asCharOrNull() = ${5000.asCharOrNull()}" }
        "5000".asChar() shouldBeEqualTo 5000.toChar(); 5000.asChar() shouldBeEqualTo 5000.toChar()
        3.14.asCharOrNull() shouldBeEqualTo 3.toChar()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random int to char`(@RandomValue(type = Char::class, size = 100) expects: List<Char>) {
        expects.forEach { expected -> expected.toString().asChar() shouldBeEqualTo expected }
    }

    @Test
    fun `convert any to byte and short`() {
        val nullByte: Byte? = null
        1.toByte().asByte() shouldBeEqualTo 1.toByte(); nullByte.asByte() shouldBeEqualTo ZERO_BYTE
        "".asByte() shouldBeEqualTo ZERO_BYTE; "C".asByte() shouldBeEqualTo ZERO_BYTE; "\t".asByte() shouldBeEqualTo ZERO_BYTE
        1.asByte() shouldBeEqualTo 1.toByte(); "1".asByte() shouldBeEqualTo 1.toByte()
        "5000".asByte() shouldBeEqualTo ZERO_BYTE; 5000.asByteOrNull() shouldBeEqualTo 5000.toByte()
        3.14.asByteOrNull() shouldBeEqualTo 3.toByte()

        val nullShort: Short? = null
        1.toShort().asShort() shouldBeEqualTo 1.toShort(); nullShort.asShort() shouldBeEqualTo 0.toShort()
        "".asShort() shouldBeEqualTo 0.toShort(); "C".asShort() shouldBeEqualTo 0.toShort()
        1.asShort() shouldBeEqualTo 1.toShort(); "1".asShort() shouldBeEqualTo 1.toShort(); '1'.asShort() shouldBeEqualTo '1'.code.toShort()
        "5000".asShort() shouldBeEqualTo 5000.toShort(); 5000.asShort() shouldBeEqualTo 5000.toShort()
        Short.MAX_VALUE.toString().asShort() shouldBeEqualTo Short.MAX_VALUE
        3.14.asShortOrNull() shouldBeEqualTo 3.toShort()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asByte`(@RandomValue(type = Byte::class, size = 100) expects: List<Byte>) {
        expects.forEach { expected -> expected.toString().asByte() shouldBeEqualTo expected }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value to short`(@RandomValue(type = Short::class, size = 100) expects: List<Short>) {
        expects.forEach { expected -> expected.toString().asShort() shouldBeEqualTo expected }
    }

    @Test
    fun `convert any to Int and Long`() {
        val nullInt: Int? = null
        "1".asInt() shouldBeEqualTo 1; nullInt.asInt() shouldBeEqualTo 0
        "".asInt() shouldBeEqualTo 0; "C".asInt() shouldBeEqualTo 0; "\t".asInt() shouldBeEqualTo 0
        1.asInt() shouldBeEqualTo 1; "1".asInt() shouldBeEqualTo 1; "5000".asInt() shouldBeEqualTo 5000; 5000.asInt() shouldBeEqualTo 5000
        Int.MAX_VALUE.toString().asInt() shouldBeEqualTo Int.MAX_VALUE; Int.MIN_VALUE.toString().asInt() shouldBeEqualTo Int.MIN_VALUE
        3.14.asIntOrNull() shouldBeEqualTo 3

        val nullLong: Long? = null
        "1".asLong() shouldBeEqualTo 1L; nullLong.asLong() shouldBeEqualTo 0L
        "".asLong() shouldBeEqualTo 0L; "C".asLong() shouldBeEqualTo 0L; "1".asLong() shouldBeEqualTo 1L
        12.asLong() shouldBeEqualTo 12L; "5000".asLong() shouldBeEqualTo 5000L; 5000.asLong() shouldBeEqualTo 5000L
        Long.MAX_VALUE.toString().asLong() shouldBeEqualTo Long.MAX_VALUE; Long.MIN_VALUE.toString().asLong() shouldBeEqualTo Long.MIN_VALUE
        3.14.asLongOrNull() shouldBeEqualTo 3L
    }

    private fun getLongValues(): List<Arguments> = listOf(
        argumentOf("0", 0L), argumentOf("2", 2L),
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
    fun `convert random value asInt`(@RandomValue(type = Int::class, size = 100) expects: List<Int>) {
        expects.forEach { expected -> expected.toString().asInt() shouldBeEqualTo expected }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value to Long`(@RandomValue(type = Long::class, size = 100) expects: List<Long>) {
        expects.forEach { expected -> expected.toString().asLong() shouldBeEqualTo expected }
    }

    @Test
    fun `convert any to Float and Double`() {
        val nullFloat: Float? = null
        "1".asFloat() shouldBeEqualTo 1.0F; nullFloat.asFloat() shouldBeEqualTo 0.0F
        "".asFloat() shouldBeEqualTo 0.0F; "C".asFloat() shouldBeEqualTo 0.0F; "\t".asFloat() shouldBeEqualTo 0.0F
        "5000".asFloat() shouldBeEqualTo 5000.0F; 5000.asFloat() shouldBeEqualTo 5000.0F
        Float.MAX_VALUE.toString().asFloat() shouldBeEqualTo Float.MAX_VALUE

        val nullDouble: Double? = null
        "1".asDouble() shouldBeEqualTo 1.0; nullDouble.asDouble() shouldBeEqualTo 0.0
        "".asDouble() shouldBeEqualTo 0.0; "C".asDouble() shouldBeEqualTo 0.0; "\t".asDouble() shouldBeEqualTo 0.0
        "5000".asDouble() shouldBeEqualTo 5000.0; 5000.asDouble() shouldBeEqualTo 5000.0
        Double.MAX_VALUE.toString().asDouble() shouldBeEqualTo Double.MAX_VALUE
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value to float`(@RandomValue(type = Float::class, size = 100) expects: List<Float>) {
        expects.forEach { expected -> expected.toString().asFloat() shouldBeEqualTo expected }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asDouble`(@RandomValue(type = Double::class, size = 100) expects: List<Double>) {
        expects.forEach { expected -> expected.toString().asDouble() shouldBeEqualTo expected }
    }

    @Test
    fun `convert any to BigInteger and BigDecimal`() {
        val nullBigInt: BigInteger? = null
        "1".asBigInt() shouldBeEqualTo BigInteger.ONE; nullBigInt.asBigInt() shouldBeEqualTo BigInteger.ZERO
        "".asBigInt() shouldBeEqualTo BigInteger.ZERO; "C".asBigInt() shouldBeEqualTo BigInteger.ZERO
        12.asBigInt() shouldBeEqualTo 12.toBigInteger(); "5000".asBigInt() shouldBeEqualTo 5000.toBigInteger()
        Long.MAX_VALUE.toString().asBigInt() shouldBeEqualTo Long.MAX_VALUE.toBigInt()

        val nullBigDec: BigDecimal? = null
        "1".asBigDecimal() shouldBeEqualTo BigDecimal.ONE; nullBigDec.asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        "".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO; "C".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        12.asBigDecimal() shouldBeEqualTo 12.toBigDecimal(); "5000".asBigDecimal() shouldBeEqualTo 5000.toBigDecimal()
        Double.MAX_VALUE.asBigDecimal() shouldBeEqualTo Double.MAX_VALUE.toBigDecimal()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigInt`(@RandomValue(type = BigInteger::class, size = 100) expects: List<BigInteger>) {
        expects.forEach { expected -> expected.toString().asBigInt() shouldBeEqualTo expected }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert random value asBigDecimal`(
        @RandomValue(type = BigDecimal::class, size = 100) expects: List<BigDecimal>,
    ) {
        expects.forEach { expected -> expected.toString().asBigDecimal() shouldBeEqualTo expected }
    }

    @Test
    fun `convert any to String`() {
        val nullValue: String? = null
        "1".asString() shouldBeEqualTo "1"; nullValue.asString() shouldBeEqualTo EMPTY_STRING
        "".asString() shouldBeEqualTo EMPTY_STRING; "C".asString() shouldBeEqualTo "C"; "\t".asString() shouldBeEqualTo "\t"
        'C'.asString() shouldBeEqualTo "C"; 12.asString() shouldBeEqualTo "12"; 5000.asString() shouldBeEqualTo "5000"
        Double.MAX_VALUE.asString() shouldBeEqualTo Double.MAX_VALUE.toString()
    }

    @Test
    fun `convert any to Date and Timestamp`() {
        val nullDate: Date? = null
        nullDate.asDateOrNull().shouldBeNull(); nullDate.asDate() shouldBeEqualTo Date(0L)
        val ts1 = System.currentTimeMillis(); val today1 = Date(ts1)
        ts1.asDate() shouldBeEqualTo Date(ts1); today1.asDate() shouldBeEqualTo today1

        val nullTs: Timestamp? = null
        nullTs.asTimestampOrNull().shouldBeNull(); nullTs.asTimestamp() shouldBeEqualTo Timestamp(0L)
        val epochMills = System.currentTimeMillis(); val timestamp = Timestamp(epochMills); val today = Date(timestamp.time)
        timestamp.asTimestamp() shouldBeEqualTo timestamp; today.asTimestamp() shouldBeEqualTo timestamp
        Instant.ofEpochMilli(epochMills).asTimestamp() shouldBeEqualTo timestamp; epochMills.asTimestamp() shouldBeEqualTo timestamp
    }

    @Test
    fun `convert any to Instant`() {
        val nullValue: Instant? = null
        nullValue.asInstantOrNull().shouldBeNull(); nullValue.asInstant() shouldBeEqualTo Instant.ofEpochMilli(0)
        val epochMills = System.currentTimeMillis(); val now = Instant.ofEpochMilli(epochMills)
        val timestamp = Timestamp(epochMills); val today = Date(timestamp.time)
        epochMills.asInstant() shouldBeEqualTo now; now.asInstant() shouldBeEqualTo now
        timestamp.asInstant() shouldBeEqualTo now; today.asInstant() shouldBeEqualTo now
        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asInstant() shouldBeEqualTo now
    }

    @Test
    fun `convert any to LocalDate and LocalTime`() {
        val epochMills = System.currentTimeMillis(); val now = Instant.ofEpochMilli(epochMills)
        val timestamp = Timestamp(epochMills); val today = Date(timestamp.time)

        val nullDate: LocalDate? = null
        nullDate.asLocalDateOrNull().shouldBeNull(); nullDate.asLocalDate() shouldBeEqualTo LocalDate.MIN
        val expectedDate = LocalDate.ofInstant(now, ZoneOffset.UTC)
        now.asLocalDate() shouldBeEqualTo expectedDate; epochMills.asLocalDate() shouldBeEqualTo expectedDate
        timestamp.asLocalDate() shouldBeEqualTo expectedDate; today.asLocalDate() shouldBeEqualTo expectedDate
        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDate() shouldBeEqualTo expectedDate
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDate() shouldBeEqualTo expectedDate

        val nullTime: LocalTime? = null
        nullTime.asLocalTimeOrNull().shouldBeNull(); nullTime.asLocalTime() shouldBeEqualTo LocalTime.MIN
        val expectedTime = LocalTime.ofInstant(now, ZoneOffset.UTC)
        now.asLocalTime() shouldBeEqualTo expectedTime; epochMills.asLocalTime() shouldBeEqualTo expectedTime
        timestamp.asLocalTime() shouldBeEqualTo expectedTime; today.asLocalTime() shouldBeEqualTo expectedTime
        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asLocalTime() shouldBeEqualTo expectedTime
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalTime() shouldBeEqualTo expectedTime
    }

    @Test
    fun `convert any to LocalDateTime and OffsetDateTime`() {
        val epochMills = System.currentTimeMillis(); val now = Instant.ofEpochMilli(epochMills)
        val timestamp = Timestamp(epochMills); val today = Date(timestamp.time)

        val nullLdt: LocalDateTime? = null
        nullLdt.asLocalDateTimeOrNull().shouldBeNull(); nullLdt.asLocalDateTime() shouldBeEqualTo LocalDateTime.MIN
        val expectedLdt = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        now.asLocalDateTime() shouldBeEqualTo expectedLdt; epochMills.asLocalDateTime() shouldBeEqualTo expectedLdt
        timestamp.asLocalDateTime() shouldBeEqualTo expectedLdt; today.asLocalDateTime() shouldBeEqualTo expectedLdt
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asLocalDateTime() shouldBeEqualTo expectedLdt

        val nullOdt: OffsetDateTime? = null
        nullOdt.asOffsetDateTimeOrNull().shouldBeNull(); nullOdt.asOffsetDateTime() shouldBeEqualTo OffsetDateTime.MIN
        val expectedOdt = OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
        now.asOffsetDateTime() shouldBeEqualTo expectedOdt; epochMills.asOffsetDateTime() shouldBeEqualTo expectedOdt
        timestamp.asOffsetDateTime() shouldBeEqualTo expectedOdt; today.asOffsetDateTime() shouldBeEqualTo expectedOdt
        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asOffsetDateTime() shouldBeEqualTo expectedOdt
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asOffsetDateTime() shouldBeEqualTo expectedOdt
    }

    @Test
    fun `convert any to ZonedDateTime`() {
        val nullValue: ZonedDateTime? = null
        nullValue.asZonedDateTimeOrNull().shouldBeNull()
        nullValue.asZonedDateTime() shouldBeEqualTo ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)
        val epochMills = System.currentTimeMillis(); val now = Instant.ofEpochMilli(epochMills)
        val expected = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
        val timestamp = Timestamp(epochMills); val today = Date(timestamp.time)
        now.asZonedDateTime() shouldBeEqualTo expected; epochMills.asZonedDateTime() shouldBeEqualTo expected
        timestamp.asZonedDateTime() shouldBeEqualTo expected; today.asZonedDateTime() shouldBeEqualTo expected
        LocalDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
        OffsetDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
        ZonedDateTime.ofInstant(now, ZoneOffset.UTC).asZonedDateTime() shouldBeEqualTo expected
    }

    @Test
    fun `convert any to UUID and byte array`() {
        val nullUuid: UUID? = null
        nullUuid.asUUIDOrNull().shouldBeNull(); nullUuid.asUUID() shouldBeEqualTo ZERO_UUID
        val uuidStr = "24738134-9d88-6645-4ec8-d63aa2031015"; val uuid = UUID.fromString(uuidStr)
        uuidStr.asUUID() shouldBeEqualTo uuid; uuid.toBigInt().asUUID() shouldBeEqualTo uuid

        val nullBa: ByteArray? = null
        nullBa.asByteArrayOrNull().shouldBeNull(); nullBa.asByteArray() shouldBeEqualTo emptyByteArray
        val array = byteArrayOf(1, 2, 3); array.asByteArray() shouldBeEqualTo array
        val str = "동해물과 백두산이"; str.asByteArray() shouldBeEqualTo str.toUtf8Bytes()
        42.asByteArray() shouldBeEqualTo 42.toByteArray(); 42L.asByteArray() shouldBeEqualTo 42L.toByteArray()
        val uuid2 = UUID.randomUUID(); uuid2.asByteArray() shouldBeEqualTo uuid2.toByteArray()
    }

    @Test
    fun `float floor round ceil 연산`() {
        val one = 1.0012345f; val one1 = 1.011111f; val one5 = 1.050234f; val one49 = 1.049999f; val nullF: Float? = null
        one.asFloatFloor(2) shouldBeEqualTo 1.00F; one.asFloatFloor(1) shouldBeEqualTo 1.0F
        one1.asFloatFloor(2) shouldBeEqualTo 1.01F; one5.asFloatFloor(2) shouldBeEqualTo 1.05F; one49.asFloatFloor(2) shouldBeEqualTo 1.04F
        nullF.asFloatFloor(2) shouldBeEqualTo 0.00F

        one.asFloatRound(2) shouldBeEqualTo 1.00F; one5.asFloatRound(2) shouldBeEqualTo 1.05F
        one5.asFloatRound(1) shouldBeEqualTo 1.1F; one49.asFloatRound(2) shouldBeEqualTo 1.05F
        nullF.asFloatRound(2) shouldBeEqualTo 0.00F

        one.asFloatCeil(2) shouldBeEqualTo 1.01F; one.asFloatCeil(1) shouldBeEqualTo 1.1F
        one1.asFloatCeil(2) shouldBeEqualTo 1.02F; one5.asFloatCeil(2) shouldBeEqualTo 1.06F
        nullF.asFloatCeil(2) shouldBeEqualTo 0.00F
    }

    @Test
    fun `double floor round ceil 연산`() {
        val one = 1.00123456; val one1 = 1.011111; val one5 = 1.0512341; val one49 = 1.0499999999; val nullD: Double? = null
        one.asDoubleFloor(2) shouldBeEqualTo 1.00; one.asDoubleFloor(1) shouldBeEqualTo 1.0
        one1.asDoubleFloor(2) shouldBeEqualTo 1.01; one5.asDoubleFloor(2) shouldBeEqualTo 1.05; one49.asDoubleFloor(2) shouldBeEqualTo 1.04
        nullD.asDoubleFloor(2) shouldBeEqualTo 0.00; "13567.6".asDoubleFloor(-2) shouldBeEqualTo 13500.0

        one.asDoubleRound(2) shouldBeEqualTo 1.00; one5.asDoubleRound(2) shouldBeEqualTo 1.05
        one5.asDoubleRound(1) shouldBeEqualTo 1.1; one49.asDoubleRound(2) shouldBeEqualTo 1.05
        nullD.asDoubleRound(2) shouldBeEqualTo 0.00

        one.asDoubleCeil(2) shouldBeEqualTo 1.01; one.asDoubleCeil(1) shouldBeEqualTo 1.1
        one1.asDoubleCeil(2) shouldBeEqualTo 1.02; one5.asDoubleCeil(2) shouldBeEqualTo 1.06
        nullD.asDoubleCeil(2) shouldBeEqualTo 0.00
    }
}
