package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

/**
 * RFC 4180 표준 준수 검증 테스트.
 *
 * CsvRecordReader 가 RFC 4180 의 핵심 규칙을 올바르게 구현하는지 검증합니다.
 */
class RFC4180ComplianceTest {

    companion object : KLogging()

    private fun read(csv: String, settings: CsvSettings = CsvSettings.DEFAULT, skipHeaders: Boolean = false): List<Record> =
        CsvRecordReader(settings).read(csv.byteInputStream(), skipHeaders = skipHeaders).toList()

    // ── 섹션 2: 레코드 구분 ──────────────────────────────

    @Test
    fun `CRLF 레코드 구분자 지원`() {
        val records = read("a,b\r\nc,d")
        records shouldHaveSize 2
        records[0].getString(0) shouldBeEqualTo "a"
        records[1].getString(0) shouldBeEqualTo "c"
    }

    @Test
    fun `LF 레코드 구분자 지원`() {
        val records = read("a,b\nc,d")
        records shouldHaveSize 2
    }

    @Test
    fun `CR 레코드 구분자 지원`() {
        val records = read("a,b\rc,d")
        records shouldHaveSize 2
    }

    @Test
    fun `마지막 행 뒤 개행 없이도 파싱`() {
        val records = read("x,y,z")
        records shouldHaveSize 1
        records[0].getString(2) shouldBeEqualTo "z"
    }

    // ── 섹션 3: 헤더 행 ─────────────────────────────────

    @Test
    fun `skipHeaders=true 이면 첫 행을 헤더로 처리`() {
        val records = read("name,age\nAlice,30", skipHeaders = true)
        records shouldHaveSize 1
        records[0].getString("name") shouldBeEqualTo "Alice"
        records[0].getString("age") shouldBeEqualTo "30"
    }

    @Test
    fun `skipHeaders=false 이면 모든 행을 데이터로 처리`() {
        val records = read("name,age\nAlice,30", skipHeaders = false)
        records shouldHaveSize 2
        records[0].getString(0) shouldBeEqualTo "name"
    }

    // ── 섹션 4: 필드 처리 ───────────────────────────────

    @Test
    fun `쉼표로 구분된 필드 파싱`() {
        val records = read("a,b,c")
        records[0].size shouldBeEqualTo 3
        records[0].getString(0) shouldBeEqualTo "a"
        records[0].getString(1) shouldBeEqualTo "b"
        records[0].getString(2) shouldBeEqualTo "c"
    }

    @Test
    fun `빈 필드는 null 반환 (emptyValueAsNull=true 기본값)`() {
        val records = read("a,,c")
        records[0].getString(0) shouldBeEqualTo "a"
        records[0].getString(1).shouldBeNull()
        records[0].getString(2) shouldBeEqualTo "c"
    }

    // ── 섹션 5: 인용 필드 ───────────────────────────────

    @Test
    fun `인용 필드 내 쉼표는 구분자가 아님`() {
        val records = read("\"a,b\",c")
        records[0].size shouldBeEqualTo 2
        records[0].getString(0) shouldBeEqualTo "a,b"
        records[0].getString(1) shouldBeEqualTo "c"
    }

    @Test
    fun `인용 필드 내 CRLF는 레코드 구분자가 아님`() {
        val records = read("\"line1\r\nline2\",end")
        records shouldHaveSize 1
        records[0].getString(0) shouldBeEqualTo "line1\r\nline2"
        records[0].getString(1) shouldBeEqualTo "end"
    }

    @Test
    fun `인용 필드 내 LF는 레코드 구분자가 아님`() {
        val records = read("\"line1\nline2\",end")
        records shouldHaveSize 1
        records[0].getString(0) shouldBeEqualTo "line1\nline2"
    }

    @Test
    fun `doubled-quote 이스케이프 처리`() {
        // "a""b" → a"b
        val records = read("\"a\"\"b\"")
        records[0].getString(0) shouldBeEqualTo "a\"b"
    }

    @Test
    fun `빈 인용 필드는 빈 문자열 반환 (emptyQuotedAsNull=false 기본값)`() {
        val records = read("a,\"\",b")
        records[0].getString(1) shouldBeEqualTo ""
    }

    @Test
    fun `빈 인용 필드 null 변환 설정`() {
        val settings = CsvSettings(emptyQuotedAsNull = true)
        val records = read("a,\"\",b", settings)
        records[0].getString(1).shouldBeNull()
    }

    // ── 행 번호 ──────────────────────────────────────────

    @Test
    fun `rowNumber는 1부터 시작하고 행마다 증가`() {
        val records = read("a\nb\nc")
        records[0].rowNumber shouldBeEqualTo 1L
        records[1].rowNumber shouldBeEqualTo 2L
        records[2].rowNumber shouldBeEqualTo 3L
    }

    @Test
    fun `skipHeaders=true 이면 rowNumber는 데이터 행 기준`() {
        val records = read("hdr\na\nb", skipHeaders = true)
        records[0].rowNumber shouldBeEqualTo 1L
        records[1].rowNumber shouldBeEqualTo 2L
    }
}
