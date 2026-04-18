package io.bluetape4k.csv.v2

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CsvRowTest {

    companion object : KLogging()

    private fun row(
        vararg values: String?,
        headers: List<String>? = null,
        rowNumber: Long = 1L,
    ) = CsvRow(values.toList(), headers, rowNumber)

    @Test
    fun `getString by index`() {
        val r = row("Alice", "30", null)
        r.getString(0) shouldBeEqualTo "Alice"
        r.getString(1) shouldBeEqualTo "30"
        r.getString(2).shouldBeNull()
        r.getString(99).shouldBeNull()
    }

    @Test
    fun `getString by name`() {
        val r = row("Alice", "30", headers = listOf("name", "age"))
        r.getString("name") shouldBeEqualTo "Alice"
        r.getString("age") shouldBeEqualTo "30"
        r.getString("unknown").shouldBeNull()
    }

    @Test
    fun `getInt and getLong`() {
        val r = row("42", "1000000000000")
        r.getIntOrNull(0) shouldBeEqualTo 42
        r.getLongOrNull(1) shouldBeEqualTo 1_000_000_000_000L
        r.getInt(0) shouldBeEqualTo 42
        r.getLong(1) shouldBeEqualTo 1_000_000_000_000L
    }

    @Test
    fun `getDouble and getFloat`() {
        val r = row("3.14", "2.72")
        r.getDoubleOrNull(0) shouldBeEqualTo 3.14
        r.getFloatOrNull(1)!!.toDouble() shouldBeEqualTo 2.72f.toDouble()
    }

    @Test
    fun `getBoolean`() {
        val r = row("true", "false", "TRUE")
        r.getBoolean(0) shouldBeEqualTo true
        r.getBoolean(1) shouldBeEqualTo false
        r.getBoolean(2) shouldBeEqualTo true
    }

    @Test
    fun `null field returns null`() {
        val r = row(null, "value")
        r.getString(0).shouldBeNull()
        r.getIntOrNull(0).shouldBeNull()
    }

    @Test
    fun `size`() {
        val r = row("a", "b", "c")
        r.size shouldBeEqualTo 3
    }

    @Test
    fun `rowNumber`() {
        val r = row("x", rowNumber = 7L)
        r.rowNumber shouldBeEqualTo 7L
    }

    @Test
    fun `data class copy`() {
        val r = row("Alice", "30")
        val copied = r.copy(rowNumber = 99L)
        copied.rowNumber shouldBeEqualTo 99L
        copied.values shouldBeEqualTo r.values
    }

    @Test
    fun `headers null when skipHeaders=false`() {
        val r = row("Alice", "30", headers = null)
        r.headers.shouldBeNull()
        r.getString("name").shouldBeNull()
    }

    @Test
    fun `getBigDecimalOrNull`() {
        val r = row("123456789.99")
        r.getBigDecimalOrNull(0).shouldNotBeNull()
        r.getBigDecimalOrNull(0)!!.toPlainString() shouldBeEqualTo "123456789.99"
    }
}
