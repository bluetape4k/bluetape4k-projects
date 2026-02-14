package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Readable
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.time.OffsetDateTime

class ReadableSupportTest {

    private class FakeReadable(
        private val indexValues: Map<Int, Any?>,
        private val nameValues: Map<String, Any?>,
    ): Readable {
        override fun get(index: Int): Any? = indexValues[index]
        override fun get(name: String): Any? = nameValues[name]

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any?> get(index: Int, type: Class<T>): T? = indexValues[index] as T?

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any?> get(name: String, type: Class<T>): T? = nameValues[name] as T?
    }

    private val readable = FakeReadable(
        indexValues = mapOf(
            0 to "42",
            1 to OffsetDateTime.parse("2026-02-14T10:20:30+09:00"),
            2 to "hello",
            3 to null,
        ),
        nameValues = mapOf(
            "bigInt" to "123456789012345678901234567890",
            "offsetTime" to OffsetDateTime.parse("2026-02-14T10:20:30+09:00"),
            "text" to "world",
            "nullable" to null,
        )
    )

    @Test
    fun `bigInt 변환을 지원한다`() {
        readable.bigInt(0) shouldBeEqualTo BigInteger("42")
        readable.bigInt("bigInt") shouldBeEqualTo BigInteger("123456789012345678901234567890")
    }

    @Test
    fun `zonedDateTime 변환을 지원한다`() {
        readable.zonedDateTime(1).shouldNotBeNull()
        readable.zonedDateTime("offsetTime").shouldNotBeNull()
        readable.zonedDateTimeOrNull(3) shouldBeEqualTo null
        readable.zonedDateTimeOrNull("nullable") shouldBeEqualTo null
    }

    @Test
    fun `getAs와 getAsOrNull 을 지원한다`() {
        readable.getAs<String>(2) shouldBeEqualTo "hello"
        readable.getAs<String>("text") shouldBeEqualTo "world"
        readable.getAsOrNull<String>(3) shouldBeEqualTo null
        readable.getAsOrNull<String>("nullable") shouldBeEqualTo null
    }
}
