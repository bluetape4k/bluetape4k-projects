package io.bluetape4k.csv.internal

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CsvLexerTest {

    companion object : KLogging()

    private fun lexerOf(
        csv: String,
        settings: CsvSettings = CsvSettings.DEFAULT,
        skipHeaders: Boolean = false,
    ): CsvLexer = CsvLexer(csv.reader(), settings, skipHeaders)

    // ────────────────────────────────────────
    // 기본 파싱
    // ────────────────────────────────────────

    @Test
    fun `parse simple CSV row`() {
        val lexer = lexerOf("a,b,c")
        lexer.hasNext().shouldBeTrue()

        val record = lexer.next()
        record.getString(0) shouldBeEqualTo "a"
        record.getString(1) shouldBeEqualTo "b"
        record.getString(2) shouldBeEqualTo "c"
        record.size shouldBeEqualTo 3
    }

    @Test
    fun `parse multiple rows`() {
        val lexer = lexerOf("a,b\nc,d")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[0].getString(1) shouldBeEqualTo "b"
        records[1].getString(0) shouldBeEqualTo "c"
        records[1].getString(1) shouldBeEqualTo "d"
    }

    @Test
    fun `parse quoted field with comma`() {
        // "a,b",c → ["a,b", "c"]
        val lexer = lexerOf("\"a,b\",c")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a,b"
        record.getString(1) shouldBeEqualTo "c"
    }

    @Test
    fun `parse doubled-quote escape`() {
        // "a""b" → [a"b]
        val lexer = lexerOf("\"a\"\"b\"")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a\"b"
    }

    // ────────────────────────────────────────
    // empty line 처리
    // ────────────────────────────────────────

    @Test
    fun `physical empty line is skipped when skipEmptyLines=true`() {
        // CsvSettings.DEFAULT: skipEmptyLines = true
        val lexer = lexerOf("a,b\n\nc,d")
        val records = lexer.asSequence().toList()

        // 빈 줄은 건너뜀 → 2개 레코드
        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[1].getString(0) shouldBeEqualTo "c"
    }

    @Test
    fun `physical empty line becomes single-null record when skipEmptyLines=false`() {
        val settings = CsvSettings(skipEmptyLines = false)
        val lexer = lexerOf("a,b\n\nc,d", settings)
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 3
        // 두 번째 레코드는 물리적 빈 줄 → 단일 null 필드
        records[1].size shouldBeEqualTo 1
    }

    @Test
    fun `comma-only line is NOT empty line`() {
        // ",," → 3개 null 필드 레코드 (물리적 빈 줄이 아님)
        // DEFAULT: emptyValueAsNull=true → null
        val lexer = lexerOf(",,")
        val record = lexer.next()

        record.size shouldBeEqualTo 3
        record.getString(0).shouldBeNull()
        record.getString(1).shouldBeNull()
        record.getString(2).shouldBeNull()
    }

    // ────────────────────────────────────────
    // skipHeaders
    // ────────────────────────────────────────

    @Test
    fun `skipHeaders stores first row as header metadata`() {
        val lexer = lexerOf("name,age\nAlice,30", skipHeaders = true)

        lexer.hasNext().shouldBeTrue()
        val record = lexer.next()

        record.getString("name") shouldBeEqualTo "Alice"
        record.getString("age") shouldBeEqualTo "30"
    }

    @Test
    fun `skipHeaders=false treats all rows as data`() {
        val lexer = lexerOf("name,age\nAlice,30", skipHeaders = false)
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "name"
        records[1].getString(0) shouldBeEqualTo "Alice"
    }

    @Test
    fun `headerNames returns correct headers when skipHeaders=true`() {
        val lexer = lexerOf("id,value\n1,hello", skipHeaders = true)
        val names = lexer.headerNames()

        names.shouldNotBeNull()
        names[0] shouldBeEqualTo "id"
        names[1] shouldBeEqualTo "value"
    }

    // ────────────────────────────────────────
    // emptyValueAsNull / emptyQuotedAsNull
    // ────────────────────────────────────────

    @Test
    fun `empty unquoted field returns null when emptyValueAsNull=true`() {
        // DEFAULT: emptyValueAsNull=true
        val lexer = lexerOf("a,,b")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a"
        record.getString(1).shouldBeNull()
        record.getString(2) shouldBeEqualTo "b"
    }

    @Test
    fun `empty unquoted field returns empty string when emptyValueAsNull=false`() {
        val settings = CsvSettings(emptyValueAsNull = false)
        val lexer = lexerOf("a,,b", settings)
        val record = lexer.next()

        record.getString(1) shouldBeEqualTo ""
    }

    @Test
    fun `quoted empty field returns empty string by default`() {
        // emptyQuotedAsNull=false (기본값)
        val lexer = lexerOf("a,\"\",b")
        val record = lexer.next()

        record.getString(1) shouldBeEqualTo ""
    }

    @Test
    fun `quoted empty field returns null when emptyQuotedAsNull=true`() {
        val settings = CsvSettings(emptyQuotedAsNull = true)
        val lexer = lexerOf("a,\"\",b", settings)
        val record = lexer.next()

        record.getString(1).shouldBeNull()
    }

    // ────────────────────────────────────────
    // CRLF / LF / CR 처리
    // ────────────────────────────────────────

    @Test
    fun `CRLF line terminator`() {
        val lexer = lexerOf("a,b\r\nc,d")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[1].getString(0) shouldBeEqualTo "c"
    }

    @Test
    fun `LF line terminator`() {
        val lexer = lexerOf("a,b\nc,d")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
    }

    @Test
    fun `CR line terminator`() {
        val lexer = lexerOf("a,b\rc,d")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[1].getString(0) shouldBeEqualTo "c"
    }

    @Test
    fun `last row without trailing newline`() {
        val lexer = lexerOf("x,y,z")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 1
        records[0].getString(2) shouldBeEqualTo "z"
    }

    // ────────────────────────────────────────
    // BOM
    // ────────────────────────────────────────

    @Test
    fun `BOM is stripped from beginning`() {
        // "\uFEFFa,b" → ["a", "b"] (BOM 제거됨)
        val withBom = "\uFEFFa,b"
        val lexer = lexerOf(withBom)
        val record = lexer.next()

        // BOM이 제거되어야 하므로 첫 필드가 "a"
        record.getString(0) shouldBeEqualTo "a"
        record.getString(1) shouldBeEqualTo "b"
    }

    // ────────────────────────────────────────
    // ParseException
    // ────────────────────────────────────────

    @Test
    fun `maxCharsPerColumn exceeded throws ParseException`() {
        val settings = CsvSettings(maxCharsPerColumn = 5)
        assertThrows<ParseException> {
            lexerOf("abcdefgh", settings).next()
        }
    }

    @Test
    fun `maxColumns exceeded throws ParseException`() {
        val settings = CsvSettings(maxColumns = 2)
        assertThrows<ParseException> {
            lexerOf("a,b,c", settings).next()
        }
    }

    // ────────────────────────────────────────
    // trimValues
    // ────────────────────────────────────────

    @Test
    fun `trimValues removes leading and trailing whitespace`() {
        val settings = CsvSettings(trimValues = true)
        val lexer = lexerOf(" a , b ", settings)
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a"
        record.getString(1) shouldBeEqualTo "b"
    }

    @Test
    fun `trimValues does not affect quoted fields`() {
        val settings = CsvSettings(trimValues = true)
        // 인용 필드는 trimValues 영향을 받지 않아야 함
        val lexer = lexerOf("\" a \",b", settings)
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo " a "
    }

    // ────────────────────────────────────────
    // hasNext / next 계약
    // ────────────────────────────────────────

    @Test
    fun `empty input returns no records`() {
        val lexer = lexerOf("")
        lexer.hasNext().shouldBeFalse()
    }

    @Test
    fun `hasNext is idempotent`() {
        val lexer = lexerOf("a,b")
        lexer.hasNext().shouldBeTrue()
        lexer.hasNext().shouldBeTrue()  // 두 번 호출해도 동일 결과
        lexer.next()  // 소비
        lexer.hasNext().shouldBeFalse()
    }

    @Test
    fun `rowNumber increments correctly`() {
        val lexer = lexerOf("a\nb\nc")
        val records = lexer.asSequence().toList()

        records[0].rowNumber shouldBeEqualTo 1L
        records[1].rowNumber shouldBeEqualTo 2L
        records[2].rowNumber shouldBeEqualTo 3L
    }

    @Test
    fun `quoted field with newline embedded is single record`() {
        // RFC 4180: 인용 필드 안에 개행이 있어도 하나의 레코드
        val lexer = lexerOf("\"line1\nline2\",end")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "line1\nline2"
        record.getString(1) shouldBeEqualTo "end"
    }
}
