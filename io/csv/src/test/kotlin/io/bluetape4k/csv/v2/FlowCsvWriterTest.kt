package io.bluetape4k.csv.v2

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import java.io.StringWriter

class FlowCsvWriterTest {

    companion object : KLogging()

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
}
