package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Readable
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID

class ReadableSupportTest {
    private class FakeReadable(
        private val indexValues: Map<Int, Any?>,
        private val nameValues: Map<String, Any?>,
    ): Readable {
        override fun get(index: Int): Any? = indexValues[index]

        override fun get(name: String): Any? = nameValues[name]

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any?> get(
            index: Int,
            type: Class<T>,
        ): T? = indexValues[index] as T?

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any?> get(
            name: String,
            type: Class<T>,
        ): T? = nameValues[name] as T?
    }

    private val offsetDateTime = OffsetDateTime.parse("2026-02-14T10:20:30+09:00")

    private val readable =
        FakeReadable(
            indexValues =
                mapOf(
                    0 to "42",
                    1 to offsetDateTime,
                    2 to "hello",
                    3 to null,
                    4 to 100L,
                    5 to 3.14,
                    6 to true,
                    7 to 42,
                    8 to "3.14",
                    9 to byteArrayOf(1, 2, 3)
                ),
            nameValues =
                mapOf(
                    "bigInt" to "123456789012345678901234567890",
                    "offsetTime" to offsetDateTime,
                    "text" to "world",
                    "nullable" to null,
                    "longVal" to 100L,
                    "doubleVal" to 3.14,
                    "boolVal" to true,
                    "intVal" to 42,
                    "bigDecimalVal" to "3.14",
                    "byteArrayVal" to byteArrayOf(1, 2, 3),
                    "localDate" to LocalDate.of(2026, 2, 14),
                    "localTime" to LocalTime.of(10, 20, 30),
                    "localDateTime" to LocalDateTime.of(2026, 2, 14, 10, 20, 30),
                    "instantVal" to Instant.ofEpochSecond(1739495230L)
                )
        )

    @Test
    fun `bigInt 변환을 지원한다`() {
        readable.bigInt(0) shouldBeEqualTo BigInteger("42")
        readable.bigInt("bigInt") shouldBeEqualTo BigInteger("123456789012345678901234567890")
    }

    @Test
    fun `bigIntOrNull 은 null 컬럼에 대해 null 을 반환한다`() {
        readable.bigIntOrNull(3).shouldBeNull()
        readable.bigIntOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `zonedDateTime 변환을 지원한다`() {
        readable.zonedDateTime(1).shouldNotBeNull()
        readable.zonedDateTime("offsetTime").shouldNotBeNull()
        readable.zonedDateTimeOrNull(3).shouldBeNull()
        readable.zonedDateTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `getAs와 getAsOrNull 을 지원한다`() {
        readable.getAs<String>(2) shouldBeEqualTo "hello"
        readable.getAs<String>("text") shouldBeEqualTo "world"
        readable.getAsOrNull<String>(3).shouldBeNull()
        readable.getAsOrNull<String>("nullable").shouldBeNull()
    }

    @Test
    fun `long 변환을 지원한다`() {
        readable.long(4) shouldBeEqualTo 100L
        readable.long("longVal") shouldBeEqualTo 100L
        readable.longOrNull(3).shouldBeNull()
        readable.longOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `double 변환을 지원한다`() {
        readable.double(5) shouldBeEqualTo 3.14
        readable.double("doubleVal") shouldBeEqualTo 3.14
        readable.doubleOrNull(3).shouldBeNull()
        readable.doubleOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `boolean 변환을 지원한다`() {
        readable.boolean(6).shouldBeTrue()
        readable.boolean("boolVal").shouldBeTrue()
        readable.booleanOrNull(3).shouldBeNull()
        readable.booleanOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `int 변환을 지원한다`() {
        readable.int(7) shouldBeEqualTo 42
        readable.int("intVal") shouldBeEqualTo 42
        readable.intOrNull(3).shouldBeNull()
        readable.intOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `bigDecimal 변환을 지원한다`() {
        readable.bigDecimal(8) shouldBeEqualTo BigDecimal("3.14")
        readable.bigDecimal("bigDecimalVal") shouldBeEqualTo BigDecimal("3.14")
        readable.bigDecimalOrNull(3).shouldBeNull()
        readable.bigDecimalOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `byteArray 변환을 지원한다`() {
        readable.byteArray(9) shouldBeEqualTo byteArrayOf(1, 2, 3)
        readable.byteArray("byteArrayVal") shouldBeEqualTo byteArrayOf(1, 2, 3)
        readable.byteArrayOrNull(3).shouldBeNull()
        readable.byteArrayOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `string 변환을 지원한다`() {
        readable.string(2) shouldBeEqualTo "hello"
        readable.string("text") shouldBeEqualTo "world"
        readable.stringOrNull(3).shouldBeNull()
        readable.stringOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localDate 변환을 지원한다`() {
        readable.localDate("localDate") shouldBeEqualTo LocalDate.of(2026, 2, 14)
        readable.localDateOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localTime 변환을 지원한다`() {
        readable.localTime("localTime") shouldBeEqualTo LocalTime.of(10, 20, 30)
        readable.localTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localDateTime 변환을 지원한다`() {
        readable.localDateTime("localDateTime") shouldBeEqualTo LocalDateTime.of(2026, 2, 14, 10, 20, 30)
        readable.localDateTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `offsetDateTime 변환을 지원한다`() {
        readable.offsetDateTime(1) shouldBeEqualTo offsetDateTime
        readable.offsetDateTime("offsetTime") shouldBeEqualTo offsetDateTime
        readable.offsetDateTimeOrNull(3).shouldBeNull()
        readable.offsetDateTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `instant 변환을 지원한다`() {
        readable.instant("instantVal") shouldBeEqualTo Instant.ofEpochSecond(1739495230L)
        readable.instantOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `Readable 지원 함수의 JVM overload를 실제 변환 경로로 검증한다`() {
        val supportClass = Class.forName("io.bluetape4k.r2dbc.support.ReadableSupportKt")

        supportClass.declaredMethods
            .asSequence()
            .filter { method -> Modifier.isStatic(method.modifiers) }
            .filter { method -> method.parameterTypes.size == 2 }
            .filter { method -> method.name !in setOf("getAs", "getAsOrNull") }
            .filter { method -> method.parameterTypes[0] == Readable::class.java }
            .forEach { method ->
                val nullable = method.name.endsWith("OrNull")
                val value = readableValue(method.name, nullable)
                val target =
                    FakeReadable(
                        indexValues = mapOf(0 to value),
                        nameValues = mapOf("value" to value),
                    )
                val argument = if (method.parameterTypes[1] == Int::class.javaPrimitiveType) 0 else "value"

                val result = method.invoke(null, target, argument)
                if (nullable) {
                    result.shouldBeNull()
                } else {
                    result.shouldNotBeNull()
                }
            }
    }

    private fun readableValue(
        methodName: String,
        nullable: Boolean,
    ): Any? {
        if (nullable) return null

        return when {
            methodName.startsWith("boolean") -> true
            methodName.startsWith("char") -> "A"
            methodName.startsWith("byteArray") -> byteArrayOf(1, 2, 3)
            methodName.startsWith("byte") -> 7.toByte()
            methodName.startsWith("short") -> 8.toShort()
            methodName.startsWith("int") -> 9
            methodName.startsWith("long") -> 10L
            methodName.startsWith("float") -> 1.25f
            methodName.startsWith("double") -> 2.5
            methodName.startsWith("bigInt") -> "123"
            methodName.startsWith("bigDecimal") -> "1.25"
            methodName.startsWith("string") -> "text"
            methodName.startsWith("date") -> Date.from(Instant.parse("2026-02-14T01:20:30Z"))
            methodName.startsWith("timestamp") -> Timestamp.from(Instant.parse("2026-02-14T01:20:30Z"))
            methodName.startsWith("instant") -> Instant.parse("2026-02-14T01:20:30Z")
            methodName.startsWith("localDateTime") -> LocalDateTime.of(2026, 2, 14, 10, 20, 30)
            methodName.startsWith("localDate") -> LocalDate.of(2026, 2, 14)
            methodName.startsWith("localTime") -> LocalTime.of(10, 20, 30)
            methodName.startsWith("offsetDateTime") -> OffsetDateTime.parse("2026-02-14T10:20:30+09:00")
            methodName.startsWith("zonedDateTime") -> ZonedDateTime.parse("2026-02-14T10:20:30+09:00[Asia/Seoul]")
            methodName.startsWith("uuid") -> UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            else -> error("지원하지 않는 Readable 함수: $methodName")
        }
    }
}
