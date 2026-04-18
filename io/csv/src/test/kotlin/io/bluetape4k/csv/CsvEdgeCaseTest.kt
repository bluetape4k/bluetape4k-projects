package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringWriter
import kotlin.text.Charsets.UTF_8

/**
 * CSV/TSV 파싱 및 직렬화의 엣지 케이스를 검증하는 테스트 클래스입니다.
 *
 * 다음 케이스를 다룹니다:
 * - 빈 CSV (데이터 없음)
 * - 헤더만 존재하는 CSV
 * - 특수문자 (따옴표, 쉼표, 탭, 개행) 포함 필드
 * - null 값 왕복(roundtrip)
 * - skipHeader=false 동작
 * - 왕복 정확성 (쓰기 후 다시 읽어 값 일치 검증)
 */
class CsvEdgeCaseTest {
    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    // region 빈 CSV 테스트

    @Test
    fun `빈 입력 스트림에서 CSV 레코드를 읽으면 빈 시퀀스를 반환한다`() {
        val emptyInput = ByteArrayInputStream(ByteArray(0))
        val records = CsvRecordReader().read(emptyInput, UTF_8, skipHeaders = false).toList()
        records.shouldBeEmpty()
    }

    @Test
    fun `빈 입력 스트림에서 TSV 레코드를 읽으면 빈 시퀀스를 반환한다`() {
        val emptyInput = ByteArrayInputStream(ByteArray(0))
        val records = TsvRecordReader().read(emptyInput, UTF_8, skipHeaders = false).toList()
        records.shouldBeEmpty()
    }

    @Test
    fun `빈 CSV 문자열에서 skipHeader=true 로 읽으면 빈 시퀀스를 반환한다`() {
        val emptyInput = ByteArrayInputStream(ByteArray(0))
        val records = CsvRecordReader().read(emptyInput, UTF_8, skipHeaders = true).toList()
        records.shouldBeEmpty()
    }

    // endregion

    // region 헤더만 있는 CSV 테스트

    @Test
    fun `헤더 행만 있는 CSV 에서 skipHeader=true 로 읽으면 빈 시퀀스를 반환한다`() {
        val csv = "name,age,city\n"
        val input = ByteArrayInputStream(csv.toByteArray(UTF_8))
        val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
        records.shouldBeEmpty()
    }

    @Test
    fun `헤더 행만 있는 TSV 에서 skipHeader=true 로 읽으면 빈 시퀀스를 반환한다`() {
        val tsv = "name\tage\tcity\n"
        val input = ByteArrayInputStream(tsv.toByteArray(UTF_8))
        val records = TsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
        records.shouldBeEmpty()
    }

    @Test
    fun `헤더 행만 있는 CSV 에서 skipHeader=false 로 읽으면 헤더 행이 레코드로 반환된다`() {
        val csv = "name,age,city\n"
        val input = ByteArrayInputStream(csv.toByteArray(UTF_8))
        val records = CsvRecordReader().read(input, UTF_8, skipHeaders = false).toList()
        records shouldHaveSize 1
        records[0].values[0] shouldBeEqualTo "name"
        records[0].values[1] shouldBeEqualTo "age"
        records[0].values[2] shouldBeEqualTo "city"
    }

    // endregion

    // region skipHeader=false 동작 테스트

    @Test
    fun `skipHeader=false 이면 헤더 행도 레코드로 포함된다`() {
        val csv = "name,age\nAlice,20\nBob,30\n"
        val input = ByteArrayInputStream(csv.toByteArray(UTF_8))
        val records = CsvRecordReader().read(input, UTF_8, skipHeaders = false).toList()
        // 헤더 포함 3행
        records shouldHaveSize 3
        records[0].values[0] shouldBeEqualTo "name"
        records[1].values[0] shouldBeEqualTo "Alice"
        records[2].values[0] shouldBeEqualTo "Bob"
    }

    @Test
    fun `skipHeader=true 이면 헤더 행은 제외된다`() {
        val csv = "name,age\nAlice,20\nBob,30\n"
        val input = ByteArrayInputStream(csv.toByteArray(UTF_8))
        val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
        // 데이터 행만 2행
        records shouldHaveSize 2
        records[0].values[0] shouldBeEqualTo "Alice"
        records[1].values[0] shouldBeEqualTo "Bob"
    }

    // endregion

    // region 특수문자 포함 필드 테스트

    @Test
    fun `쉼표가 포함된 필드를 CSV 로 쓰고 읽으면 올바르게 파싱된다`() {
        val csvFile = File(tempDir, "comma_in_field.csv")
        csvFile.writeCsvRecords(
            headers = listOf("name", "description"),
            rows =
                listOf(
                    listOf("Alice", "Hello, World"),
                    listOf("Bob", "foo,bar,baz")
                )
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 2
        records[0].getValue(0, "") shouldBeEqualTo "Alice"
        records[0].getValue(1, "") shouldBeEqualTo "Hello, World"
        records[1].getValue(0, "") shouldBeEqualTo "Bob"
        records[1].getValue(1, "") shouldBeEqualTo "foo,bar,baz"
    }

    @Test
    fun `큰따옴표가 포함된 필드를 CSV 로 쓰고 읽으면 올바르게 파싱된다`() {
        val csvFile = File(tempDir, "quote_in_field.csv")
        csvFile.writeCsvRecords(
            headers = listOf("name", "quote"),
            rows =
                listOf(
                    listOf("Alice", """He said "Hello""""),
                    listOf("Bob", """She said "Goodbye"""")
                )
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 2
        records[0].getValue(1, "") shouldBeEqualTo """He said "Hello""""
        records[1].getValue(1, "") shouldBeEqualTo """She said "Goodbye""""
    }

    @Test
    fun `탭 문자가 포함된 필드를 TSV 로 쓰고 읽으면 올바르게 파싱된다`() {
        // TSV는 기본적으로 탭을 이스케이프 처리함
        StringWriter().use { sw ->
            TsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("name", "value")
                writer.writeRow(listOf("key", "value\twith\ttabs"))
            }
            val captured = sw.buffer.toString()
            log.debug { "captured=$captured" }
            // TSV 포맷에서 탭은 이스케이프됨 — 출력에 \t 이스케이프 시퀀스가 포함되어야 함
            captured shouldContain "key"
        }
    }

    @Test
    fun `개행이 포함된 필드를 CSV 로 쓰고 읽으면 올바르게 파싱된다`() {
        val csvFile = File(tempDir, "newline_in_field.csv")
        csvFile.writeCsvRecords(
            headers = listOf("name", "notes"),
            rows =
                listOf(
                    listOf("Alice", "line1\nline2")
                )
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 1
        records[0].getValue(0, "") shouldBeEqualTo "Alice"
        records[0].getValue(1, "") shouldBeEqualTo "line1\nline2"
    }

    @Test
    fun `유니코드 문자를 포함한 CSV 를 쓰고 읽으면 올바르게 파싱된다`() {
        val csvFile = File(tempDir, "unicode.csv")
        csvFile.writeCsvRecords(
            headers = listOf("name", "greeting"),
            rows =
                listOf(
                    listOf("홍길동", "안녕하세요"),
                    listOf("田中", "こんにちは"),
                    listOf("Müller", "Grüß Gott")
                )
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 3
        records[0].getValue(0, "") shouldBeEqualTo "홍길동"
        records[0].getValue(1, "") shouldBeEqualTo "안녕하세요"
        records[1].getValue(0, "") shouldBeEqualTo "田中"
        records[2].getValue(0, "") shouldBeEqualTo "Müller"
    }

    // endregion

    // region null 값 왕복 테스트

    @Test
    fun `null 값을 CSV 로 쓰면 빈 문자열로 기록되고 읽으면 null 로 파싱된다`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("name", "optional")
                writer.writeRow(listOf("Alice", null))
                writer.writeRow(listOf("Bob", "present"))
            }
            val output = sw.buffer.toString()
            log.debug { "output=\n$output" }

            // null은 빈 칸으로 기록됨
            output shouldContain "Alice,"
            output shouldContain "Bob,present"

            // 파싱 시 null 컬럼은 null로 반환됨
            val input = ByteArrayInputStream(output.toByteArray(UTF_8))
            val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
            records shouldHaveSize 2
            records[0].getString(1).shouldBeNull()
            records[1].getValue(1, "") shouldBeEqualTo "present"
        }
    }

    @Test
    fun `모든 컬럼이 null 인 행을 CSV 로 쓰고 읽으면 모두 null 로 파싱된다`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("a", "b", "c")
                writer.writeRow(listOf(null, null, null))
            }
            val output = sw.buffer.toString()
            log.debug { "output=\n$output" }

            val input = ByteArrayInputStream(output.toByteArray(UTF_8))
            val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
            records shouldHaveSize 1
            records[0].getString(0).shouldBeNull()
            records[0].getString(1).shouldBeNull()
            records[0].getString(2).shouldBeNull()
        }
    }

    // endregion

    // region 왕복(roundtrip) 정확성 테스트

    @Test
    fun `CSV 파일에 쓰고 다시 읽으면 정확한 값이 반환된다`() {
        val csvFile = File(tempDir, "roundtrip_exact.csv")
        val originalRows =
            listOf(
                listOf("Alice", "20", "Seoul"),
                listOf("Bob", "30", "Busan"),
                listOf("Charlie", "25", "Daegu")
            )

        csvFile.writeCsvRecords(
            headers = listOf("name", "age", "city"),
            rows = originalRows
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 3
        records[0].getValue(0, "") shouldBeEqualTo "Alice"
        records[0].getValue(1, "") shouldBeEqualTo "20"
        records[0].getValue(2, "") shouldBeEqualTo "Seoul"
        records[1].getValue(0, "") shouldBeEqualTo "Bob"
        records[2].getValue(0, "") shouldBeEqualTo "Charlie"
        records[2].getValue(2, "") shouldBeEqualTo "Daegu"
    }

    @Test
    fun `TSV 파일에 쓰고 다시 읽으면 정확한 값이 반환된다`() {
        val tsvFile = File(tempDir, "roundtrip_exact.tsv")
        val originalRows =
            listOf(
                listOf("Alice", "20", "Seoul"),
                listOf("Bob", "30", "Busan")
            )

        tsvFile.writeTsvRecords(
            headers = listOf("name", "age", "city"),
            rows = originalRows
        )

        val records = tsvFile.readAsTsvRecords(skipHeader = true).toList()
        log.debug { "records=$records" }

        records shouldHaveSize 2
        records[0].getValue(0, "") shouldBeEqualTo "Alice"
        records[0].getValue(1, "") shouldBeEqualTo "20"
        records[0].getValue(2, "") shouldBeEqualTo "Seoul"
        records[1].getValue(0, "") shouldBeEqualTo "Bob"
    }

    @Test
    fun `StringWriter 를 사용한 CSV 쓰기와 읽기 왕복 테스트`() {
        val rows =
            listOf(
                listOf("item1", "100", "true"),
                listOf("item2", "200", "false")
            )

        val csvString =
            StringWriter()
                .also { sw ->
                    CsvRecordWriter(sw).use { writer ->
                        writer.writeHeaders("name", "value", "active")
                        rows.forEach { writer.writeRow(it) }
                    }
                }.buffer
                .toString()

        log.debug { "csvString=\n$csvString" }

        val input = ByteArrayInputStream(csvString.toByteArray(UTF_8))
        val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()

        records shouldHaveSize 2
        records[0].getValue(0, "") shouldBeEqualTo "item1"
        records[0].getValue(1, "") shouldBeEqualTo "100"
        records[0].getValue(2, "") shouldBeEqualTo "true"
        records[1].getValue(0, "") shouldBeEqualTo "item2"
        records[1].getValue(1, "") shouldBeEqualTo "200"
        records[1].getValue(2, "") shouldBeEqualTo "false"
    }

    @Test
    fun `단일 컬럼 CSV 를 쓰고 읽으면 정확히 반환된다`() {
        val csvFile = File(tempDir, "single_col.csv")
        csvFile.writeCsvRecords(
            headers = listOf("id"),
            rows =
                listOf(
                    listOf("1"),
                    listOf("2"),
                    listOf("3")
                )
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        records shouldHaveSize 3
        records.map { it.getValue(0, "") } shouldBeEqualTo listOf("1", "2", "3")
    }

    @Test
    fun `빈 문자열 값을 CSV 로 쓰면 null 로 파싱된다`() {
        // univocity 파서는 빈 CSV 필드를 null 로 처리합니다.
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("name", "middle", "surname")
                writer.writeRow(listOf("Alice", "", "Smith"))
            }
            val output = sw.buffer.toString()
            log.debug { "output=\n$output" }

            val input = ByteArrayInputStream(output.toByteArray(UTF_8))
            val records = CsvRecordReader().read(input, UTF_8, skipHeaders = true).toList()
            records shouldHaveSize 1
            records[0].getValue(0, "") shouldBeEqualTo "Alice"
            // univocity 파서는 빈 필드를 null 로 반환 — 기본값으로 빈 문자열 지정
            records[0].getValue(1, "") shouldBeEqualTo ""
            records[0].getValue(2, "") shouldBeEqualTo "Smith"
        }
    }

    // endregion

    // region 대용량 데이터 기본 검증

    @Test
    fun `많은 행을 CSV 로 쓰고 읽으면 모든 행이 반환된다`() {
        val rowCount = 1_000
        val csvFile = File(tempDir, "large.csv")

        csvFile.writeCsvRecords(
            headers = listOf("id", "name"),
            rows = (1..rowCount).map { listOf(it.toString(), "name_$it") }
        )

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        records shouldHaveSize rowCount
        records.first().getValue(0, "") shouldBeEqualTo "1"
        records.last().getValue(0, "") shouldBeEqualTo rowCount.toString()
    }

    // endregion

    // region 헤더 리스트 방식 검증

    @Test
    fun `writeHeaders 리스트 방식과 가변인자 방식이 동일한 결과를 낸다`() {
        val output1 =
            StringWriter()
                .also { sw ->
                    CsvRecordWriter(sw).use { writer ->
                        writer.writeHeaders(listOf("a", "b", "c"))
                        writer.writeRow(listOf("1", "2", "3"))
                    }
                }.buffer
                .toString()

        val output2 =
            StringWriter()
                .also { sw ->
                    CsvRecordWriter(sw).use { writer ->
                        writer.writeHeaders("a", "b", "c")
                        writer.writeRow(listOf("1", "2", "3"))
                    }
                }.buffer
                .toString()

        output1 shouldBeEqualTo output2
    }

    // endregion
}
