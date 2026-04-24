package io.bluetape4k.csv.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Coroutines CSV/TSV 엣지 케이스 테스트입니다.
 *
 * 다음 케이스를 다룹니다:
 * - 빈 입력 스트림
 * - 헤더만 있는 입력
 * - 특수문자(쉼표, 따옴표, 유니코드) 포함 필드
 * - null 값 왕복(roundtrip)
 * - 파일 기반 왕복 정확성
 */
class SuspendCsvEdgeCaseTest {
    companion object: KLoggingChannel()

    @TempDir
    lateinit var tempDir: File

    // region 빈 입력 테스트

    @Test
    fun `빈 입력 스트림에서 suspend CSV 읽기는 빈 Flow 를 반환한다`() =
        runTest {
            val emptyInput = ByteArrayInputStream(ByteArray(0))
            val records = emptyInput.readAsCsvRecordsSuspending(skipHeader = false).toList()
            records.shouldBeEmpty()
        }

    @Test
    fun `빈 입력 스트림에서 suspend TSV 읽기는 빈 Flow 를 반환한다`() =
        runTest {
            val emptyInput = ByteArrayInputStream(ByteArray(0))
            val records = emptyInput.readAsTsvRecordsSuspending(skipHeader = false).toList()
            records.shouldBeEmpty()
        }

    // endregion

    // region 헤더만 있는 입력 테스트

    @Test
    fun `헤더 행만 있는 CSV 에서 skipHeader=true 이면 빈 Flow 를 반환한다`() =
        runTest {
            val csv = "name,age,city\n"
            val input = ByteArrayInputStream(csv.toByteArray(UTF_8))
            val records = input.readAsCsvRecordsSuspending(skipHeader = true).toList()
            records.shouldBeEmpty()
        }

    @Test
    fun `헤더 행만 있는 TSV 에서 skipHeader=true 이면 빈 Flow 를 반환한다`() =
        runTest {
            val tsv = "name\tage\tcity\n"
            val input = ByteArrayInputStream(tsv.toByteArray(UTF_8))
            val records = input.readAsTsvRecordsSuspending(skipHeader = true).toList()
            records.shouldBeEmpty()
        }

    // endregion

    // region 특수문자 포함 필드 왕복 테스트

    @Test
    fun `쉼표가 포함된 필드를 suspend CSV 로 쓰고 읽으면 올바르게 파싱된다`() =
        runTest {
            val csvFile = File(tempDir, "comma_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "description"),
                rows =
                    listOf(
                        listOf("Alice", "Hello, World"),
                        listOf("Bob", "foo,bar,baz")
                    )
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
            log.debug { "records=$records" }

            records shouldHaveSize 2
            records[0].getValue(0, "") shouldBeEqualTo "Alice"
            records[0].getValue(1, "") shouldBeEqualTo "Hello, World"
            records[1].getValue(1, "") shouldBeEqualTo "foo,bar,baz"
        }

    @Test
    fun `큰따옴표가 포함된 필드를 suspend CSV 로 쓰고 읽으면 올바르게 파싱된다`() =
        runTest {
            val csvFile = File(tempDir, "quote_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "quote"),
                rows =
                    listOf(
                        listOf("Alice", """He said "Hello""""),
                        listOf("Bob", """She said "Goodbye"""")
                    )
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
            log.debug { "records=$records" }

            records shouldHaveSize 2
            records[0].getValue(1, "") shouldBeEqualTo """He said "Hello""""
            records[1].getValue(1, "") shouldBeEqualTo """She said "Goodbye""""
        }

    @Test
    fun `유니코드 문자를 포함한 suspend CSV 를 쓰고 읽으면 올바르게 파싱된다`() =
        runTest {
            val csvFile = File(tempDir, "unicode_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "greeting"),
                rows =
                    listOf(
                        listOf("홍길동", "안녕하세요"),
                        listOf("田中", "こんにちは"),
                        listOf("Müller", "Grüß Gott")
                    )
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
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
    fun `null 값이 포함된 CSV 를 suspend 로 쓰고 읽으면 null 로 파싱된다`() =
        runTest {
            val csvFile = File(tempDir, "null_values_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "optional"),
                rows =
                    listOf(
                        listOf("Alice", null),
                        listOf("Bob", "present")
                    )
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
            log.debug { "records=$records" }

            records shouldHaveSize 2
            records[0].getValue(0, "") shouldBeEqualTo "Alice"
            records[0].getString(1).shouldBeNull()
            records[1].getValue(0, "") shouldBeEqualTo "Bob"
            records[1].getValue(1, "") shouldBeEqualTo "present"
        }

    // endregion

    // region 파일 기반 왕복 정확성 테스트

    @Test
    fun `suspend CSV 파일에 쓰고 다시 읽으면 정확한 값이 반환된다`() =
        runTest {
            val csvFile = File(tempDir, "roundtrip_suspend.csv")
            val originalRows =
                listOf(
                    listOf("Alice", "20", "Seoul"),
                    listOf("Bob", "30", "Busan"),
                    listOf("Charlie", "25", "Daegu")
                )

            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "age", "city"),
                rows = originalRows
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
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
    fun `suspend TSV 파일에 쓰고 다시 읽으면 정확한 값이 반환된다`() =
        runTest {
            val tsvFile = File(tempDir, "roundtrip_suspend.tsv")
            val originalRows =
                listOf(
                    listOf("Alice", "20", "Seoul"),
                    listOf("Bob", "30", "Busan")
                )

            tsvFile.writeTsvRecordsSuspending(
                headers = listOf("name", "age", "city"),
                rows = originalRows
            )

            val records = tsvFile.readAsTsvRecordsSuspending(skipHeader = true).toList()
            log.debug { "records=$records" }

            records shouldHaveSize 2
            records[0].getValue(0, "") shouldBeEqualTo "Alice"
            records[0].getValue(1, "") shouldBeEqualTo "20"
            records[0].getValue(2, "") shouldBeEqualTo "Seoul"
            records[1].getValue(0, "") shouldBeEqualTo "Bob"
        }

    @Test
    fun `엔티티를 suspend CSV 로 쓰고 읽으면 정확한 값이 반환된다`() =
        runTest {
            data class Item(
                val id: Int,
                val label: String,
            )

            val items = listOf(Item(1, "first"), Item(2, "second"), Item(3, "third"))
            val csvFile = File(tempDir, "entities_suspend.csv")

            csvFile.writeCsvRecordsSuspending(
                headers = listOf("id", "label"),
                entities = items
            ) { item -> listOf(item.id.toString(), item.label) }

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
            records shouldHaveSize 3
            records[0].getValue(0, "") shouldBeEqualTo "1"
            records[0].getValue(1, "") shouldBeEqualTo "first"
            records[2].getValue(0, "") shouldBeEqualTo "3"
            records[2].getValue(1, "") shouldBeEqualTo "third"
        }

    @Test
    fun `단일 컬럼 suspend CSV 를 쓰고 읽으면 정확히 반환된다`() =
        runTest {
            val csvFile = File(tempDir, "single_col_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("id"),
                rows = (1..5).map { listOf(it.toString()) }
            )

            val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()
            records shouldHaveSize 5
            records.map { it.getValue(0, "") } shouldBeEqualTo listOf("1", "2", "3", "4", "5")
        }

    @Test
    fun `transform 을 사용한 suspend CSV 읽기가 정확한 값을 반환한다`() =
        runTest {
            val csvFile = File(tempDir, "transform_suspend.csv")
            csvFile.writeCsvRecordsSuspending(
                headers = listOf("name", "age"),
                rows =
                    listOf(
                        listOf("Alice", "20"),
                        listOf("Bob", "30")
                    )
            )

            val names =
                csvFile
                    .readAsCsvRecordsSuspending(skipHeader = true) { record ->
                        record.getValue(0, "")
                    }.toList()

            names.shouldNotBeEmpty()
            names shouldBeEqualTo listOf("Alice", "Bob")
        }

    // endregion
}
