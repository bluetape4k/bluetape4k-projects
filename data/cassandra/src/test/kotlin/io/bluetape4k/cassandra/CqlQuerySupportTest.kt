package io.bluetape4k.cassandra

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class CqlQuerySupportTest {

    @Test
    fun `quote and unquote 는 역변환이 가능하다`() {
        val raw = "Simpson's family"
        val quoted = raw.quote()

        quoted shouldBeEqualTo "'Simpson''s family'"
        quoted.isQuoted().shouldBeTrue()
        quoted.unquote() shouldBeEqualTo raw
    }

    @Test
    fun `doubleQuote and unDoubleQuote 는 역변환이 가능하다`() {
        val raw = "class=\"content\""
        val quoted = raw.doubleQuote()

        quoted shouldBeEqualTo "\"class=\"\"content\"\"\""
        quoted.isDoubleQuoted().shouldBeTrue()
        quoted.unDoubleQuote() shouldBeEqualTo raw
    }

    @Test
    fun `quote 는 null 입력 시 빈문자열 리터럴을 반환한다`() {
        (null as String?).quote() shouldBeEqualTo "''"
    }

    @Test
    fun `needsDoubleQuotes 는 식별자 형태 여부를 판별한다`() {
        "valid_identifier".needsDoubleQuotes().shouldBeFalse()
        "invalid identifier".needsDoubleQuotes().shouldBeTrue()
    }
}
