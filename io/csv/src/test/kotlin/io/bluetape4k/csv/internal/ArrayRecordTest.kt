package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class ArrayRecordTest {

    companion object : KLogging()

    private fun makeRecord(
        values: Array<String?> = arrayOf("a", "b", "c"),
        headers: Array<String>? = null,
        headerIndex: HeaderIndex? = null,
        rowNumber: Long = 1L,
    ): ArrayRecord = ArrayRecord(
        rawValues = values,
        _headers = headers,
        headerIndex = headerIndex,
        rowNumber = rowNumber,
    )

    @Test
    fun `getString by index returns raw value`() {
        val record = makeRecord(arrayOf("hello", "world", null))

        record.getString(0) shouldBeEqualTo "hello"
        record.getString(1) shouldBeEqualTo "world"
        record.getString(2).shouldBeNull()
    }

    @Test
    fun `getString returns null for out of range index`() {
        val record = makeRecord(arrayOf("a", "b"))

        record.getString(99).shouldBeNull()
        record.getString(-1).shouldBeNull()
    }

    @Test
    fun `getString by name requires headerIndex`() {
        val headers = arrayOf("id", "name")
        val headerIndex = HeaderIndex.of(headers)
        val record = makeRecord(
            values = arrayOf("42", "Alice"),
            headers = headers,
            headerIndex = headerIndex,
        )

        record.getString("id") shouldBeEqualTo "42"
        record.getString("name") shouldBeEqualTo "Alice"
    }

    @Test
    fun `getString by name returns null when headerIndex is null`() {
        val record = makeRecord(arrayOf("a", "b"), headers = null, headerIndex = null)

        record.getString("any").shouldBeNull()
    }

    @Test
    fun `getString by name returns null for unknown column name`() {
        val headers = arrayOf("id")
        val headerIndex = HeaderIndex.of(headers)
        val record = makeRecord(arrayOf("1"), headers = headers, headerIndex = headerIndex)

        record.getString("unknown").shouldBeNull()
    }

    @Test
    fun `getValue Int with default`() {
        val record = makeRecord(arrayOf("42", "not-a-number"))

        record.getValue(0, 0) shouldBeEqualTo 42
        record.getValue(1, -1) shouldBeEqualTo -1  // 변환 실패 → defaultValue
    }

    @Test
    fun `getValue Long with default`() {
        val record = makeRecord(arrayOf("9999999999", "bad"))

        record.getValue(0, 0L) shouldBeEqualTo 9999999999L
        record.getValue(1, -1L) shouldBeEqualTo -1L
    }

    @Test
    fun `getValue with parse failure returns defaultValue`() {
        val record = makeRecord(arrayOf(null, "xyz"))

        record.getValue(0, 0) shouldBeEqualTo 0       // null → defaultValue
        record.getValue(1, 99) shouldBeEqualTo 99     // 변환 불가 → defaultValue
    }

    @Test
    fun `getIntOrNull returns null on parse failure`() {
        val record = makeRecord(arrayOf("42", "abc", null))

        record.getIntOrNull(0) shouldBeEqualTo 42
        record.getIntOrNull(1).shouldBeNull()
        record.getIntOrNull(2).shouldBeNull()
    }

    @Test
    fun `defensive copy - mutation of original array not reflected`() {
        val original = arrayOf<String?>("a", "b", "c")
        val record = makeRecord(original)

        original[0] = "MUTATED"

        // ArrayRecord는 방어적 복사이므로 원본 변경이 반영되지 않아야 함
        record.getString(0) shouldBeEqualTo "a"
    }

    @Test
    fun `values returns defensive copy`() {
        val record = makeRecord(arrayOf("x", "y"))

        val v1 = record.values
        val v2 = record.values

        v1 shouldBeEqualTo v2
        (v1 === v2).shouldBeFalse()  // 매번 새 배열 반환
    }

    @Test
    fun `rowNumber stored correctly`() {
        val record = makeRecord(rowNumber = 77L)
        record.rowNumber shouldBeEqualTo 77L
    }

    @Test
    fun `size equals rawValues length`() {
        val record = makeRecord(arrayOf("a", "b", "c", "d"))
        record.size shouldBeEqualTo 4
    }

    @Test
    fun `getValue by name with header lookup`() {
        val headers = arrayOf("score", "label")
        val headerIndex = HeaderIndex.of(headers)
        val record = makeRecord(
            values = arrayOf("95", "A"),
            headers = headers,
            headerIndex = headerIndex,
        )

        record.getValue("score", 0) shouldBeEqualTo 95
        record.getValue("label", "") shouldBeEqualTo "A"
        record.getValue("nonexistent", -1) shouldBeEqualTo -1  // missing key → defaultValue
    }

    @Test
    fun `getLongOrNull returns null on parse failure`() {
        val record = makeRecord(arrayOf("12345", "nope", null))

        record.getLongOrNull(0) shouldBeEqualTo 12345L
        record.getLongOrNull(1).shouldBeNull()
        record.getLongOrNull(2).shouldBeNull()
    }
}
