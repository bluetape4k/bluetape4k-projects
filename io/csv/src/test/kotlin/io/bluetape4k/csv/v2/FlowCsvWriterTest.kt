package io.bluetape4k.csv.v2

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.StringWriter
import java.nio.file.Path

class FlowCsvWriterTest {

    companion object : KLogging()

    @TempDir
    lateinit var tempDir: Path

    private fun writerOf(block: CsvWriterConfig.() -> Unit = {}): Pair<StringWriter, FlowCsvWriter> {
        val sw = StringWriter()
        return sw to csvWriter(sw, block)
    }

    private fun tsvWriterOf(block: CsvWriterConfig.() -> Unit = {}): Pair<StringWriter, FlowCsvWriter> {
        val sw = StringWriter()
        return sw to tsvWriter(sw, block)
    }

    // ── basic write ──────────────────────────────────────

    @Test
    fun `writeRow produces CSV line`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeRow(listOf("Alice", "30"))
        writer.close()

        sw.toString() shouldBeEqualTo "Alice,30\r\n"
    }

    @Test
    fun `writeHeaders and writeRow`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeHeaders(listOf("name", "age"))
        writer.writeRow(listOf("Alice", 30))
        writer.close()

        val lines = sw.toString().split("\r\n").filter { it.isNotEmpty() }
        lines[0] shouldBeEqualTo "name,age"
        lines[1] shouldBeEqualTo "Alice,30"
    }

    @Test
    fun `null field is written as empty unquoted`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeRow(listOf("a", null, "c"))
        writer.close()

        sw.toString() shouldBeEqualTo "a,,c\r\n"
    }

    @Test
    fun `empty string field is written as quoted empty`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeRow(listOf("a", "", "c"))
        writer.close()

        sw.toString() shouldBeEqualTo """a,"",c""" + "\r\n"
    }

    @Test
    fun `field with comma is quoted`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeRow(listOf("hello, world", "42"))
        writer.close()

        sw.toString() shouldBeEqualTo """"hello, world",42""" + "\r\n"
    }

    @Test
    fun `field with quote char is double-quoted`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeRow(listOf("say \"hi\""))
        writer.close()

        // say "hi" → "say ""hi"""  (RFC 4180 doubled-quote + surrounding quotes)
        sw.toString() shouldBeEqualTo "\"say \"\"hi\"\"\"\r\n"
    }

    // ── TSV writer ───────────────────────────────────────

    @Test
    fun `tsvWriter uses tab delimiter`() = runTest {
        val (sw, writer) = tsvWriterOf()
        writer.writeRow(listOf("a", "b", "c"))
        writer.close()

        sw.toString() shouldBeEqualTo "a\tb\tc\n"
    }

    @Test
    fun `tsvWriter delimiter cannot be overridden to comma`() = runTest {
        val (sw, writer) = tsvWriterOf { delimiter = ',' }
        writer.writeRow(listOf("x", "y"))
        writer.close()

        sw.toString() shouldBeEqualTo "x\ty\n"
    }

    // ── quoteAll ─────────────────────────────────────────

    @Test
    fun `quoteAll wraps all non-null fields`() = runTest {
        val (sw, writer) = writerOf { quoteAll = true }
        writer.writeRow(listOf("Alice", "30", null))
        writer.close()

        sw.toString() shouldBeEqualTo """"Alice","30",""" + "\r\n"
    }

    @Test
    fun `quoteAll with embedded quote uses doubled-quote`() = runTest {
        val (sw, writer) = writerOf { quoteAll = true }
        writer.writeRow(listOf("say \"hi\""))
        writer.close()

        // say "hi" → "say ""hi"""  (RFC 4180 doubled-quote + surrounding quotes)
        sw.toString() shouldBeEqualTo "\"say \"\"hi\"\"\"\r\n"
    }

    // ── writeAll (Flow) ──────────────────────────────────

    @Test
    fun `writeAll collects flow rows`() = runTest {
        val (sw, writer) = writerOf()
        writer.writeAll(
            flowOf(
                listOf("Alice", 30),
                listOf("Bob", 25),
            )
        )
        writer.close()

        val output = sw.toString()
        output shouldContain "Alice,30"
        output shouldContain "Bob,25"
    }

    // ── custom delimiter ─────────────────────────────────

    @Test
    fun `semicolon delimiter`() = runTest {
        val sw = StringWriter()
        val writer = csvWriter(sw) { delimiter = ';' }
        writer.writeRow(listOf("a", "b", "c"))
        writer.close()

        sw.toString() shouldBeEqualTo "a;b;c\r\n"
    }

    // ── writeFile(Path) ──────────────────────────────────

    @Test
    fun `writeFile writes rows to file and returns count`() = runTest {
        val file = tempDir.resolve("output.csv")
        val sw = StringWriter()
        val writer = csvWriter(sw)

        val count = writer.writeFile(
            path = file,
            skipHeaders = true,
            rows = flowOf(listOf("Alice", 30), listOf("Bob", 25)),
        )

        count shouldBeEqualTo 2L
        val lines = file.toFile().readLines()
        lines[0] shouldBeEqualTo "Alice,30"
        lines[1] shouldBeEqualTo "Bob,25"
    }

    @Test
    fun `writeFile writes headers when skipHeaders=false`() = runTest {
        val file = tempDir.resolve("with_headers.csv")
        val sw = StringWriter()
        val writer = csvWriter(sw)

        val count = writer.writeFile(
            path = file,
            skipHeaders = false,
            headers = listOf("name", "age"),
            rows = flowOf(listOf("Alice", 30)),
        )

        count shouldBeEqualTo 1L
        val lines = file.toFile().readLines()
        lines[0] shouldBeEqualTo "name,age"
        lines[1] shouldBeEqualTo "Alice,30"
    }

    @Test
    fun `writeFile append mode adds rows to existing file`() = runTest {
        val file = tempDir.resolve("append.csv")
        file.toFile().writeText("Alice,30\r\n")
        val sw = StringWriter()
        val writer = csvWriter(sw)

        val count = writer.writeFile(
            path = file,
            append = true,
            skipHeaders = true,
            rows = flowOf(listOf("Bob", 25)),
        )

        count shouldBeEqualTo 1L
        val content = file.toFile().readText()
        content shouldContain "Alice,30"
        content shouldContain "Bob,25"
    }

    @Test
    fun `writeFile returns zero for empty flow`() = runTest {
        val file = tempDir.resolve("empty.csv")
        val sw = StringWriter()
        val writer = csvWriter(sw)

        val count = writer.writeFile(
            path = file,
            skipHeaders = true,
            rows = flowOf(),
        )

        count shouldBeEqualTo 0L
    }

    // ── close() flushes underlying writer (regression: cff1141c7) ──────
    @Test
    fun `close flushes buffered OutputStreamWriter to file`() = runTest {
        val file = tempDir.resolve("buffered.csv")
        val bufferedWriter = java.io.BufferedWriter(
            java.io.OutputStreamWriter(
                java.io.FileOutputStream(file.toFile()),
                Charsets.UTF_8,
            ),
        )
        val writer = csvWriter(bufferedWriter)
        writer.writeRow(listOf("Alice", 30))
        writer.writeRow(listOf("Bob", 25))
        writer.close()

        // close() must flush+close so data reaches the file even with a BufferedWriter underneath
        val content = file.toFile().readText(Charsets.UTF_8)
        content shouldContain "Alice,30"
        content shouldContain "Bob,25"
    }

    @Test
    fun `close is idempotent and swallows exceptions`() = runTest {
        val (_, writer) = writerOf()
        writer.writeRow(listOf("x"))
        writer.close()
        // second close must not throw (runCatching in FlowCsvWriterImpl.close)
        writer.close()
    }
}
