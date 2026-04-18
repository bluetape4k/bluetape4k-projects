package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.StringWriter

class DelimitedWriterTest {

    companion object : KLogging()

    private fun writerOf(lineSep: String = "\r\n"): Pair<StringWriter, DelimitedWriter> {
        val sw = StringWriter()
        val dw = DelimitedWriter(
            writer = sw,
            delimiter = ',',
            quote = '"',
            quoteEscape = '"',
            lineSeparator = lineSep,
        )
        return sw to dw
    }

    @Test
    fun `writeRow writes simple fields`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("a", "b", "c"))
        dw.close()

        sw.toString() shouldBeEqualTo "a,b,c\r\n"
    }

    @Test
    fun `null field produces empty unquoted field`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("a", null, "b"))
        dw.close()

        sw.toString() shouldBeEqualTo "a,,b\r\n"
    }

    @Test
    fun `empty string produces quoted empty field`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("a", "", "b"))
        dw.close()

        // 빈 문자열은 "" 인용 출력 (roundtrip 보장)
        sw.toString() shouldBeEqualTo "a,\"\",b\r\n"
    }

    @Test
    fun `field with delimiter gets quoted`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("a", "b,c", "d"))
        dw.close()

        sw.toString() shouldBeEqualTo "a,\"b,c\",d\r\n"
    }

    @Test
    fun `field with quote char gets doubled`() {
        val (sw, dw) = writerOf()
        // a"b → RFC 4180: "a""b"
        dw.writeRow(listOf("a\"b"))
        dw.close()

        sw.toString() shouldBeEqualTo "\"a\"\"b\"\r\n"
    }

    @Test
    fun `field with newline gets quoted`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("line1\nline2"))
        dw.close()

        sw.toString() shouldBeEqualTo "\"line1\nline2\"\r\n"
    }

    @Test
    fun `field with CR gets quoted`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("a\rb"))
        dw.close()

        sw.toString() shouldBeEqualTo "\"a\rb\"\r\n"
    }

    @Test
    fun `leading space triggers quoting`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf(" leading"))
        dw.close()

        sw.toString() shouldBeEqualTo "\" leading\"\r\n"
    }

    @Test
    fun `trailing space triggers quoting`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("trailing "))
        dw.close()

        sw.toString() shouldBeEqualTo "\"trailing \"\r\n"
    }

    @Test
    fun `needsQuoting returns false for plain field`() {
        val (_, dw) = writerOf()
        dw.needsQuoting("hello").shouldBeFalse()
        dw.needsQuoting("123").shouldBeFalse()
        dw.close()
    }

    @Test
    fun `needsQuoting returns true for field with delimiter`() {
        val (_, dw) = writerOf()
        dw.needsQuoting("a,b").shouldBeTrue()
        dw.close()
    }

    @Test
    fun `needsQuoting returns true for field with quote char`() {
        val (_, dw) = writerOf()
        dw.needsQuoting("a\"b").shouldBeTrue()
        dw.close()
    }

    @Test
    fun `needsQuoting returns false for empty string`() {
        val (_, dw) = writerOf()
        // needsQuoting("") == false (호출자가 별도 처리)
        dw.needsQuoting("").shouldBeFalse()
        dw.close()
    }

    @Test
    fun `close flushes and closes writer`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf("x"))
        dw.close()
        // close 후에도 StringWriter 내용 접근 가능
        sw.toString() shouldBeEqualTo "x\r\n"
    }

    @Test
    fun `null vs empty string roundtrip`() {
        // null → write → 비인용 빈 필드
        // "" → write → "" 인용 출력
        val (sw, dw) = writerOf()
        dw.writeRow(listOf(null, ""))
        dw.close()

        val result = sw.toString()
        // null이 먼저, "" 가 두 번째
        result shouldBeEqualTo ",\"\"\r\n"
    }

    @Test
    fun `multiple rows each terminated by lineSeparator`() {
        val (sw, dw) = writerOf("\n")
        dw.writeRow(listOf("r1c1", "r1c2"))
        dw.writeRow(listOf("r2c1", "r2c2"))
        dw.close()

        sw.toString() shouldBeEqualTo "r1c1,r1c2\nr2c1,r2c2\n"
    }

    @Test
    fun `non-string field uses toString`() {
        val (sw, dw) = writerOf()
        dw.writeRow(listOf(42, 3.14, true))
        dw.close()

        sw.toString() shouldBeEqualTo "42,3.14,true\r\n"
    }
}
