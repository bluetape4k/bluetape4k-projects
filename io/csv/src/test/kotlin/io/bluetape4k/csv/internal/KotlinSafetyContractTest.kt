package io.bluetape4k.csv.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.TsvSettings
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test

class KotlinSafetyContractTest {

    @Test
    fun `all lexer implementations preserve Iterator EOF contract`() {
        CsvLexer("".reader(), CsvSettings.DEFAULT).use { lexer ->
            lexer.hasNext().shouldBeFalse()
            assertFailsWith<NoSuchElementException> { lexer.next() }
        }
        TsvLexer("".reader(), TsvSettings.DEFAULT).use { lexer ->
            lexer.hasNext().shouldBeFalse()
            assertFailsWith<NoSuchElementException> { lexer.next() }
        }
        OkioCsvLexer("".byteInputStream().source().buffer(), CsvSettings.DEFAULT).use { lexer ->
            lexer.hasNext().shouldBeFalse()
            assertFailsWith<NoSuchElementException> { lexer.next() }
        }
    }

    @Test
    fun `settings reject invalid limits through the shared validation contract`() {
        assertFailsWith<IllegalArgumentException> { CsvSettings(maxCharsPerColumn = 0) }
        assertFailsWith<IllegalArgumentException> { CsvSettings(maxColumns = 0) }
        assertFailsWith<IllegalArgumentException> { CsvSettings(bufferSize = 0) }
        assertFailsWith<IllegalArgumentException> { CsvSettings(lineSeparator = "") }
        assertFailsWith<IllegalArgumentException> { CsvSettings(quoteEscape = '\\') }

        assertFailsWith<IllegalArgumentException> { TsvSettings(maxCharsPerColumn = 0) }
        assertFailsWith<IllegalArgumentException> { TsvSettings(maxColumns = 0) }
        assertFailsWith<IllegalArgumentException> { TsvSettings(bufferSize = 0) }
        assertFailsWith<IllegalArgumentException> { TsvSettings(lineSeparator = "") }
    }

    @Test
    fun `valid settings continue to support both lexer paths`() {
        val csv = CsvLexer("name\nvalue".reader(), CsvSettings.DEFAULT, skipHeaders = true)
        val okio = OkioCsvLexer(
            "name\nvalue".byteInputStream().source().buffer(),
            CsvSettings.DEFAULT,
            skipHeaders = true,
        )
        csv.use { lexer -> lexer.hasNext().shouldBeTrue() }
        okio.use { lexer -> lexer.hasNext().shouldBeTrue() }
    }
}
