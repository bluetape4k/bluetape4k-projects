package io.bluetape4k.csv.v2

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CsvRowTest {

    companion object: KLogging()

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

    @Test
    fun `getInt with default falls back on null and invalid`() {
        val r = row(null, "not-a-number", "7", headers = listOf("a", "b", "c"))
        r.getInt(0, default = -1) shouldBeEqualTo -1
        r.getInt(1, default = -1) shouldBeEqualTo -1
        r.getInt(2, default = -1) shouldBeEqualTo 7
        r.getInt("a", default = 99) shouldBeEqualTo 99
        r.getInt("c", default = 99) shouldBeEqualTo 7
    }

    @Test
    fun `getLong with default falls back on null and invalid`() {
        val r = row(null, "xx", "1234567890", headers = listOf("a", "b", "c"))
        r.getLong(0, default = -1L) shouldBeEqualTo -1L
        r.getLong(1, default = -1L) shouldBeEqualTo -1L
        r.getLong("c", default = 0L) shouldBeEqualTo 1_234_567_890L
    }

    @Test
    fun `getDouble with default falls back on null and invalid`() {
        val r = row(null, "NaN-ish", "2.5", headers = listOf("a", "b", "c"))
        r.getDouble(0, default = 1.0) shouldBeEqualTo 1.0
        r.getDouble(1, default = 1.0) shouldBeEqualTo 1.0
        r.getDouble("c", default = 0.0) shouldBeEqualTo 2.5
    }

    @Test
    fun `getFloat with default falls back on null and invalid`() {
        val r = row(null, "not-float", "0.5", headers = listOf("a", "b", "c"))
        r.getFloat(0, default = 1f) shouldBeEqualTo 1f
        r.getFloat(1, default = 1f) shouldBeEqualTo 1f
        r.getFloat(2, default = 0f) shouldBeEqualTo 0.5f
        r.getFloat("c", default = 9f) shouldBeEqualTo 0.5f
        r.getFloat("a", default = 9f) shouldBeEqualTo 9f
    }

    @Test
    fun `getBigDecimal with default falls back on null and invalid`() {
        val r = row(null, "not-decimal", "100.25", headers = listOf("a", "b", "c"))
        r.getBigDecimal(0, default = BigDecimal.ONE) shouldBeEqualTo BigDecimal.ONE
        r.getBigDecimal(1, default = BigDecimal.ONE) shouldBeEqualTo BigDecimal.ONE
        r.getBigDecimal(2, default = BigDecimal.ZERO).toPlainString() shouldBeEqualTo "100.25"
        r.getBigDecimal("c", default = BigDecimal.ZERO).toPlainString() shouldBeEqualTo "100.25"
        r.getBigDecimal("a", default = BigDecimal.TEN) shouldBeEqualTo BigDecimal.TEN
    }

    @Test
    fun `getBoolean with default by name`() {
        val r = row("true", null, "invalid", headers = listOf("a", "b", "c"))
        r.getBoolean("a", default = false) shouldBeEqualTo true
        r.getBoolean("b", default = true) shouldBeEqualTo true
        r.getBoolean("c", default = true) shouldBeEqualTo false
    }
}
