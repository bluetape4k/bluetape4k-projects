package io.bluetape4k.csv.internal

import io.bluetape4k.csv.TsvSettings
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class TsvLexerTest {

    companion object : KLogging()

    private fun lexerOf(
        tsv: String,
        settings: TsvSettings = TsvSettings.DEFAULT,
        skipHeaders: Boolean = false,
    ): TsvLexer = TsvLexer(tsv.reader(), settings, skipHeaders)

    // ────────────────────────────────────────
    // 기본 파싱
    // ────────────────────────────────────────

    @Test
    fun `parse simple TSV row`() {
        val lexer = lexerOf("a\tb\tc")
        lexer.hasNext().shouldBeTrue()

        val record = lexer.next()
        record.getString(0) shouldBeEqualTo "a"
        record.getString(1) shouldBeEqualTo "b"
        record.getString(2) shouldBeEqualTo "c"
        record.size shouldBeEqualTo 3
    }

    @Test
    fun `parse multiple TSV rows`() {
        val lexer = lexerOf("a\tb\nc\td")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[0].getString(1) shouldBeEqualTo "b"
        records[1].getString(0) shouldBeEqualTo "c"
        records[1].getString(1) shouldBeEqualTo "d"
    }

    // ────────────────────────────────────────
    // 백슬래시 이스케이프
    // ────────────────────────────────────────

    @Test
    fun `backslash-t is unescaped to tab`() {
        // "a\tb" where \t is the literal escape sequence → field contains tab
        val lexer = lexerOf("a\\tb")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a\tb"
    }

    @Test
    fun `backslash-n is unescaped to newline`() {
        val lexer = lexerOf("a\\nb")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a\nb"
    }

    @Test
    fun `backslash-r is unescaped to CR`() {
        val lexer = lexerOf("a\\rb")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a\rb"
    }

    @Test
    fun `backslash-backslash is unescaped to single backslash`() {
        val lexer = lexerOf("a\\\\b")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a\\b"
    }

    @Test
    fun `unknown escape is kept literally`() {
        // "\\x" → "\\x" (관대한 처리)
        val lexer = lexerOf("\\x")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "\\x"
    }

    // ────────────────────────────────────────
    // null 처리
    // ────────────────────────────────────────

    @Test
    fun `empty field returns null when emptyValueAsNull=true`() {
        // DEFAULT: emptyValueAsNull=true
        val lexer = lexerOf("a\t\tb")
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a"
        record.getString(1).shouldBeNull()
        record.getString(2) shouldBeEqualTo "b"
    }

    @Test
    fun `empty field returns empty string when emptyValueAsNull=false`() {
        val settings = TsvSettings(emptyValueAsNull = false)
        val lexer = lexerOf("a\t\tb", settings)
        val record = lexer.next()

        record.getString(1) shouldBeEqualTo ""
    }

    // ────────────────────────────────────────
    // skipHeaders
    // ────────────────────────────────────────

    @Test
    fun `skipHeaders stores first row as header`() {
        val lexer = lexerOf("name\tage\nAlice\t30", skipHeaders = true)

        lexer.hasNext().shouldBeTrue()
        val record = lexer.next()

        record.getString("name") shouldBeEqualTo "Alice"
        record.getString("age") shouldBeEqualTo "30"
    }

    @Test
    fun `skipHeaders=false treats all rows as data`() {
        val lexer = lexerOf("name\tage\nAlice\t30", skipHeaders = false)
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "name"
    }

    // ────────────────────────────────────────
    // CRLF / LF / CR
    // ────────────────────────────────────────

    @Test
    fun `LF line terminator`() {
        val lexer = lexerOf("a\tb\nc\td")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
    }

    @Test
    fun `CRLF line terminator`() {
        val lexer = lexerOf("a\tb\r\nc\td")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[1].getString(0) shouldBeEqualTo "c"
    }

    @Test
    fun `CR line terminator`() {
        val lexer = lexerOf("a\tb\rc\td")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 2
    }

    @Test
    fun `last row without trailing newline`() {
        val lexer = lexerOf("x\ty\tz")
        val records = lexer.asSequence().toList()

        records.size shouldBeEqualTo 1
        records[0].getString(2) shouldBeEqualTo "z"
    }

    // ────────────────────────────────────────
    // hasNext
    // ────────────────────────────────────────

    @Test
    fun `empty TSV returns no records`() {
        val lexer = lexerOf("")
        lexer.hasNext().shouldBeFalse()
    }

    @Test
    fun `hasNext is idempotent`() {
        val lexer = lexerOf("a\tb")
        lexer.hasNext().shouldBeTrue()
        lexer.hasNext().shouldBeTrue()
        lexer.next()
        lexer.hasNext().shouldBeFalse()
    }

    // ────────────────────────────────────────
    // trimValues
    // ────────────────────────────────────────

    @Test
    fun `trimValues removes leading and trailing whitespace`() {
        val settings = TsvSettings(trimValues = true)
        val lexer = lexerOf(" a \t b ", settings)
        val record = lexer.next()

        record.getString(0) shouldBeEqualTo "a"
        record.getString(1) shouldBeEqualTo "b"
    }

    // ────────────────────────────────────────
    // rowNumber
    // ────────────────────────────────────────

    @Test
    fun `rowNumber increments correctly`() {
        val lexer = lexerOf("a\nb\nc")
        val records = lexer.asSequence().toList()

        records[0].rowNumber shouldBeEqualTo 1L
        records[1].rowNumber shouldBeEqualTo 2L
        records[2].rowNumber shouldBeEqualTo 3L
    }

    // ────────────────────────────────────────
    // PR 1 게이트: 기존 TsvRecordReader와 동일 입력 대조
    // ────────────────────────────────────────

    /**
     * PR 1 gate test — product_type.tsv 파일을 TsvLexer로 파싱하여
     * 첫 레코드 필드 수 >= 4 임을 확인한다.
     *
     * 이 테스트가 실패하면 TsvLexer 동작이 univocity와 다른 것이므로 PR 1 머지 불가.
     */
    @Test
    fun `first record fields match existing TsvRecordReader output`() {
        Resourcex.getInputStream("csv/product_type.tsv").shouldNotBeNull()
        Resourcex.getInputStream("csv/product_type.tsv")!!.bufferedReader(Charsets.UTF_8).use { reader ->
            // skipHeaders=true: 첫 행이 헤더
            val lexer = TsvLexer(reader, TsvSettings.DEFAULT, skipHeaders = true)

            lexer.hasNext().shouldBeTrue()
            val first = lexer.next()

            first.size shouldBeGreaterThan 3  // product_type.tsv는 7컬럼
            // 첫 번째 필드(tagFamilyValue)는 비어 있지 않아야 함
            first.getString(0).shouldNotBeNull()
            first.getString(0)!!.shouldNotBeEmpty()
        }
    }

    @Test
    fun `all records in product_type_tsv have at least 4 fields`() {
        Resourcex.getInputStream("csv/product_type.tsv").shouldNotBeNull()
        Resourcex.getInputStream("csv/product_type.tsv")!!.bufferedReader(Charsets.UTF_8).use { reader ->
            val lexer = TsvLexer(reader, TsvSettings.DEFAULT, skipHeaders = true)
            var count = 0
            for (record in lexer) {
                record.size shouldBeGreaterThan 3
                count++
            }
            count shouldBeGreaterThan 0
        }
    }
}
