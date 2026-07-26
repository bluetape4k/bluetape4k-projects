package io.bluetape4k.csv.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.utils.Resourcex
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test
import kotlin.text.Charsets.UTF_8

class OkioCsvLexerTest {

    @Test
    fun `Okio lexer matches Reader lexer for RFC4180 records`() {
        val csv = """
            name,description,amount
            apple,"red, fresh",10
            banana,"multi
            line",20
            quote,"a ""quoted"" field",30
            unicode,한글과 emoji 🚀,40
        """.trimIndent()

        parseWithOkio(csv) shouldBeEqualTo parseWithReader(csv)
    }

    @Test
    fun `Okio lexer preserves BOM stripping and empty value policies`() {
        val csv = "\uFEFFname,empty,quoted\nalpha,,\"\"\n"

        parseWithOkio(csv) shouldBeEqualTo parseWithReader(csv)
    }

    @Test
    fun `Okio lexer supports tab delimiter`() {
        val settings = CsvSettings.DEFAULT.copy(delimiter = '\t')
        val tsv = "name\tvalue\nalpha\t1\nbeta\t2\n"

        parseWithOkio(tsv, settings) shouldBeEqualTo parseWithReader(tsv, settings)
    }

    @Test
    fun `Okio lexer matches Reader lexer for large fixture`() {
        val csv = Resourcex.getInputStream("csv/extra_words.csv")!!.readBytes().toString(UTF_8)
        val okioRecords = parseWithOkio(csv)
        val readerRecords = parseWithReader(csv)

        okioRecords.zip(readerRecords).indexOfFirst { (okio, reader) -> okio != reader } shouldBeEqualTo -1
        okioRecords.size shouldBeEqualTo readerRecords.size
    }

    @Test
    fun `Okio lexer enforces maxCharsPerColumn without unbounded read ahead`() {
        val settings = CsvSettings.DEFAULT.copy(maxCharsPerColumn = 5)

        assertFailsWith<ParseException> {
            OkioCsvLexer(
                "name\nabcdef\n".byteInputStream().source().buffer(),
                settings,
                skipHeaders = true
            ).use { lexer ->
                lexer.next()
            }
        }
    }

    private fun parseWithOkio(
        text: String,
        settings: CsvSettings = CsvSettings.DEFAULT,
    ): List<List<String?>> =
        OkioCsvLexer(text.byteInputStream().source().buffer(), settings, skipHeaders = true).use { lexer ->
            buildList {
                while (lexer.hasNext()) {
                    add(lexer.next().values.toList())
                }
            }
        }

    private fun parseWithReader(
        text: String,
        settings: CsvSettings = CsvSettings.DEFAULT,
    ): List<List<String?>> =
        CsvLexer(text.reader(), settings, skipHeaders = true).use { lexer ->
            buildList {
                while (lexer.hasNext()) {
                    add(lexer.next().values.toList())
                }
            }
        }
}
