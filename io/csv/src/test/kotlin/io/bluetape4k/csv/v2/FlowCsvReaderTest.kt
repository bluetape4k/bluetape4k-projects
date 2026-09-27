package io.bluetape4k.csv.v2

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path

class FlowCsvReaderTest {

    companion object: KLoggingChannel()

    @TempDir
    lateinit var tempDir: Path

    private fun streamOf(csv: String) = ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))

    // ── DSL builder ──────────────────────────────────────

    @Test
    fun `csvReader default settings`() = runSuspendIO {
        val reader = csvReader()
        reader.config.delimiter shouldBeEqualTo ','
        reader.config.quote shouldBeEqualTo '"'
    }

    @Test
    fun `tsvReader forces tab delimiter`() = runSuspendIO {
        val reader = tsvReader()
        reader.config.delimiter shouldBeEqualTo '\t'
    }

    @Test
    fun `tsvReader block cannot override delimiter`() = runSuspendIO {
        val reader = tsvReader { delimiter = ',' }
        reader.config.delimiter shouldBeEqualTo '\t'
    }

    @Test
    fun `csvReader custom delimiter`() = runSuspendIO {
        val reader = csvReader { delimiter = ';' }
        reader.config.delimiter shouldBeEqualTo ';'
    }

    // ── basic reading ────────────────────────────────────

    @Test
    fun `read simple CSV`() = runSuspendIO {
        val reader = csvReader()
        val rows = reader.read(streamOf("Alice,30\nBob,25")).toList()

        rows shouldHaveSize 2
        rows[0].getString(0) shouldBeEqualTo "Alice"
        rows[0].getString(1) shouldBeEqualTo "30"
        rows[1].getString(0) shouldBeEqualTo "Bob"
    }

    @Test
    fun `read CSV with headers`() = runSuspendIO {
        val reader = csvReader()
        val rows = reader.read(streamOf("name,age\nAlice,30\nBob,25"), skipHeaders = true).toList()

        rows shouldHaveSize 2
        rows[0].headers.shouldNotBeNull()
        rows[0].getString("name") shouldBeEqualTo "Alice"
        rows[0].getString("age") shouldBeEqualTo "30"
        rows[1].getString("name") shouldBeEqualTo "Bob"
    }

    @Test
    fun `read TSV with tsvReader`() = runSuspendIO {
        val reader = tsvReader()
        val rows = reader.read(streamOf("a\tb\tc\n1\t2\t3"), skipHeaders = true).toList()

        rows shouldHaveSize 1
        rows[0].getString("a") shouldBeEqualTo "1"
        rows[0].getString("b") shouldBeEqualTo "2"
    }

    @Test
    fun `read empty input returns empty flow`() = runSuspendIO {
        val rows = csvReader().read(streamOf("")).toList()
        rows shouldHaveSize 0
    }

    @Test
    fun `null field when emptyValueAsNull=true`() = runSuspendIO {
        val rows = csvReader().read(streamOf("a,,c")).toList()
        rows[0].getString(0) shouldBeEqualTo "a"
        rows[0].getString(1).shouldBeNull()
        rows[0].getString(2) shouldBeEqualTo "c"
    }

    @Test
    fun `empty string field when emptyValueAsNull=false`() = runSuspendIO {
        val reader = csvReader { emptyValueAsNull = false }
        val rows = reader.read(streamOf("a,,c")).toList()
        rows[0].getString(1) shouldBeEqualTo ""
    }

    @Test
    fun `quoted field with comma`() = runSuspendIO {
        val rows = csvReader().read(streamOf(""""hello, world",42""")).toList()
        rows[0].getString(0) shouldBeEqualTo "hello, world"
        rows[0].getString(1) shouldBeEqualTo "42"
    }

    @Test
    fun `trimValues removes whitespace`() = runSuspendIO {
        val reader = csvReader { trimValues = true }
        val rows = reader.read(streamOf(" Alice , 30 ")).toList()
        rows[0].getString(0) shouldBeEqualTo "Alice"
        rows[0].getString(1) shouldBeEqualTo "30"
    }

    @Test
    fun `rowNumber increments correctly`() = runSuspendIO {
        val rows = csvReader().read(streamOf("a\nb\nc")).toList()
        rows[0].rowNumber shouldBeEqualTo 1L
        rows[1].rowNumber shouldBeEqualTo 2L
        rows[2].rowNumber shouldBeEqualTo 3L
    }

    // ── Record.toCsvRow() roundtrip ──────────────────────

    @Test
    fun `Record toCsvRow roundtrip`() = runSuspendIO {
        val rows = csvReader().read(streamOf("name,age\nAlice,30"), skipHeaders = true).toList()
        val row = rows[0]
        val record = row.toRecord()

        record.getString("name") shouldBeEqualTo "Alice"
        record.getString("age") shouldBeEqualTo "30"

        val backToRow = record.toCsvRow()
        backToRow.getString("name") shouldBeEqualTo "Alice"
        backToRow.headers.shouldNotBeNull()
        backToRow.headers shouldHaveSize 2
    }

    // ── readFile(Path) ────────────────────────────────────

    @Test
    fun `readFile reads CSV from file`() = runSuspendIO {
        val file = tempDir.resolve("test.csv")
        file.toFile().writeText("Alice,30\nBob,25", Charsets.UTF_8)

        val rows = csvReader().readFile(file).toList()

        rows shouldHaveSize 2
        rows[0].getString(0) shouldBeEqualTo "Alice"
        rows[0].getString(1) shouldBeEqualTo "30"
        rows[1].getString(0) shouldBeEqualTo "Bob"
    }

    @Test
    fun `readFile reads CSV with headers from file`() = runSuspendIO {
        val file = tempDir.resolve("test_headers.csv")
        file.toFile().writeText("name,age\nAlice,30\nBob,25", Charsets.UTF_8)

        val rows = csvReader().readFile(file, skipHeaders = true).toList()

        rows shouldHaveSize 2
        rows[0].headers.shouldNotBeNull()
        rows[0].getString("name") shouldBeEqualTo "Alice"
        rows[0].getString("age") shouldBeEqualTo "30"
        rows[1].getString("name") shouldBeEqualTo "Bob"
    }

    @Test
    fun `readFile reads empty file returns empty flow`() = runSuspendIO {
        val file = tempDir.resolve("empty.csv")
        file.toFile().writeText("", Charsets.UTF_8)

        val rows = csvReader().readFile(file).toList()
        rows shouldHaveSize 0
    }

    @Test
    fun `readFile rowNumber increments correctly`() = runSuspendIO {
        val file = tempDir.resolve("rows.csv")
        file.toFile().writeText("a\nb\nc", Charsets.UTF_8)

        val rows = csvReader().readFile(file).toList()

        rows shouldHaveSize 3
        rows[0].rowNumber shouldBeEqualTo 1L
        rows[1].rowNumber shouldBeEqualTo 2L
        rows[2].rowNumber shouldBeEqualTo 3L
    }

    // ── BOM handling ─────────────────────────────────────

    @Test
    fun `BOM is stripped when detectBom=true`() = runSuspendIO {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val data = bom + "Alice,30".toByteArray(Charsets.UTF_8)
        val rows = csvReader { detectBom = true }.read(ByteArrayInputStream(data)).toList()

        rows.shouldNotBeEmpty()
        rows[0].getString(0) shouldBeEqualTo "Alice"
    }
}
