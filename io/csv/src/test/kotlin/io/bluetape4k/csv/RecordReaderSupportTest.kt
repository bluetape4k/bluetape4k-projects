package io.bluetape4k.csv

import com.univocity.parsers.common.record.Record
import io.bluetape4k.csv.model.ProductType
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RecordReaderSupportTest {

    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    private val productTypeMapper = { record: Record ->
        ProductType(
            tagFamily = record.getValue(0, "").trim(),
            representative = record.getValue(1, "").trim(),
            synonym = record.getValue<String?>(2, null)?.trim(),
            tagType = record.getValue<String?>(3, null)?.trim(),
            priority = record.getValue<Int?>(4, null),
            parentRepresentativeValue = record.getValue<String?>(5, null)?.trim(),
            level = record.getValue(6, 0)
        )
    }

    @Test
    fun `File에서 CSV 레코드를 읽는다`() {
        val csvFile = createTempCsvFile()
        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()

        log.trace { "records=$records" }
        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    @Test
    fun `File에서 CSV 레코드를 transform으로 읽는다`() {
        val csvFile = createTempCsvFile()
        val names = csvFile.readAsCsvRecords(skipHeader = true) { record ->
            record.getValue(0, "").trim()
        }.toList()

        log.trace { "names=$names" }
        names.shouldNotBeEmpty()
        names.forEach { it.shouldNotBeBlank() }
    }

    @Test
    fun `File에서 TSV 레코드를 읽는다`() {
        val tsvFile = createTempTsvFile()
        val records = tsvFile.readAsTsvRecords(skipHeader = true).toList()

        log.trace { "records=$records" }
        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    @Test
    fun `File에서 TSV 레코드를 transform으로 읽는다`() {
        val tsvFile = createTempTsvFile()
        val names = tsvFile.readAsTsvRecords(skipHeader = true) { record ->
            record.getValue(0, "").trim()
        }.toList()

        log.trace { "names=$names" }
        names.shouldNotBeEmpty()
        names.forEach { it.shouldNotBeBlank() }
    }

    @Test
    fun `InputStream에서 CSV 레코드를 transform으로 읽는다`() {
        Resourcex.getInputStream("csv/product_type.csv")!!.buffered().use { bis ->
            val productTypes = bis.readAsCsvRecords(skipHeader = true, transform = productTypeMapper).toList()

            log.trace { "productTypes=$productTypes" }
            productTypes.shouldNotBeEmpty()
            productTypes.forEach {
                it.tagFamily.shouldNotBeBlank()
                it.representative.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `InputStream에서 TSV 레코드를 transform으로 읽는다`() {
        Resourcex.getInputStream("csv/product_type.tsv")!!.buffered().use { bis ->
            val productTypes = bis.readAsTsvRecords(skipHeader = true, transform = productTypeMapper).toList()

            log.trace { "productTypes=$productTypes" }
            productTypes.shouldNotBeEmpty()
            productTypes.forEach {
                it.tagFamily.shouldNotBeBlank()
                it.representative.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `File readAsCsvRecords는 Sequence 소비가 끝날때까지 스트림을 유지한다`() {
        val csvFile = createTempCsvFile()

        // Sequence를 반환한 후 별도로 소비 - 스트림이 조기에 닫히지 않아야 함
        val sequence = csvFile.readAsCsvRecords(skipHeader = true)
        val records = sequence.toList()

        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    private fun createTempCsvFile(): File {
        val csvFile = File(tempDir, "test.csv")
        csvFile.writeCsvRecords(
            headers = listOf("name", "age", "city"),
            rows = listOf(
                listOf("Alice", 20, "Seoul"),
                listOf("Bob", 30, "Busan"),
                listOf("Charlie", 25, "Daegu"),
            )
        )
        return csvFile
    }

    private fun createTempTsvFile(): File {
        val tsvFile = File(tempDir, "test.tsv")
        tsvFile.writeTsvRecords(
            headers = listOf("name", "age", "city"),
            rows = listOf(
                listOf("Alice", 20, "Seoul"),
                listOf("Bob", 30, "Busan"),
                listOf("Charlie", 25, "Daegu"),
            )
        )
        return tsvFile
    }
}
