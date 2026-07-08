package io.bluetape4k.csv.coroutines

import io.bluetape4k.csv.Record
import io.bluetape4k.csv.model.ProductType
import io.bluetape4k.csv.writeCsvRecords
import io.bluetape4k.csv.writeTsvRecords
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException

class SuspendRecordReaderSupportTest {

    companion object : KLoggingChannel()

    @TempDir
    lateinit var tempDir: File

    private val productTypeMapper: suspend (Record) -> ProductType = { record: Record ->
        ProductType(
            tagFamily = record.getValue(0, "").trim(),
            representative = record.getValue(1, "").trim(),
            synonym = record.getString(2)?.trim(),
            tagType = record.getString(3)?.trim(),
            priority = record.getIntOrNull(4),
            parentRepresentativeValue = record.getString(5)?.trim(),
            level = record.getValue(6, 0)
        )
    }

    @Test
    fun `File에서 CSV 레코드를 읽는다`() = runSuspendIO {
        val csvFile = createTempCsvFile()
        val records = csvFile.readAsCsvRecordsSuspending(skipHeader = true).toList()

        log.trace { "records=$records" }
        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    @Test
    fun `File에서 CSV 레코드를 transform으로 읽는다`() = runSuspendIO {
        val csvFile = createTempCsvFile()
        val names = csvFile.readAsCsvRecordsSuspending(skipHeader = true) { record ->
            record.getValue(0, "").trim()
        }.toList()

        log.debug { "names=$names" }
        names.shouldNotBeEmpty()
        names.forEach { it.shouldNotBeBlank() }
    }

    @Test
    fun `File에서 TSV 레코드를 읽는다`() = runSuspendIO {
        val tsvFile = createTempTsvFile()
        val records = tsvFile.readAsTsvRecordsSuspending(skipHeader = true).toList()

        log.debug { "records=$records" }
        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    @Test
    fun `File에서 TSV 레코드를 transform으로 읽는다`() = runSuspendIO {
        val tsvFile = createTempTsvFile()
        val names = tsvFile.readAsTsvRecordsSuspending(skipHeader = true) { record ->
            record.getValue(0, "").trim()
        }.toList()

        log.debug { "names=$names" }
        names.shouldNotBeEmpty()
        names.forEach { it.shouldNotBeBlank() }
    }

    @Test
    fun `InputStream에서 CSV 레코드를 transform으로 읽는다`() = runSuspendIO {
        Resourcex.getInputStream("csv/product_type.csv")!!.buffered().use { bis ->
            val productTypes = bis
                .readAsCsvRecordsSuspending(skipHeader = true, transform = productTypeMapper)
                .toList()

            log.debug { "productTypes=$productTypes" }
            productTypes.shouldNotBeEmpty()
            productTypes.forEach {
                it.tagFamily.shouldNotBeBlank()
                it.representative.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `InputStream에서 TSV 레코드를 transform으로 읽는다`() = runSuspendIO {
        Resourcex.getInputStream("csv/product_type.tsv")!!.buffered().use { bis ->
            val productTypes = bis
                .readAsTsvRecordsSuspending(skipHeader = true, transform = productTypeMapper)
                .toList()

            log.debug { "productTypes=$productTypes" }
            productTypes.shouldNotBeEmpty()
            productTypes.forEach {
                it.tagFamily.shouldNotBeBlank()
                it.representative.shouldNotBeBlank()
            }
        }
    }

    @Test
    fun `File readAsCsvRecords는 Sequence 소비가 끝날때까지 스트림을 유지한다`() = runSuspendIO {
        val csvFile = createTempCsvFile()

        val sequence = csvFile.readAsCsvRecordsSuspending(skipHeader = true)
        val records = sequence.toList()

        records.shouldNotBeEmpty()
        records.size shouldBeGreaterThan 0
    }

    @Test
    fun `File CSV suspending reader opens missing file only when collected`() = runSuspendIO {
        val missingFile = File(tempDir, "missing.csv")

        val flow = missingFile.readAsCsvRecordsSuspending(skipHeader = true)

        assertFailsWith<FileNotFoundException> {
            flow.toList()
        }
    }

    @Test
    fun `File CSV suspending flow can be collected again after early termination`() = runSuspendIO {
        val csvFile = createTempCsvFile()

        val flow = csvFile.readAsCsvRecordsSuspending(skipHeader = true)

        flow.take(1).toList() shouldHaveSize 1
        val records = flow.toList()

        records shouldHaveSize 3
        records.map { it.getValue(0, "") } shouldBeEqualTo listOf("Alice", "Bob", "Charlie")
    }

    @Test
    fun `File TSV suspending transform flow can be collected repeatedly`() = runSuspendIO {
        val tsvFile = createTempTsvFile()

        val flow = tsvFile.readAsTsvRecordsSuspending(skipHeader = true) { record ->
            record.getValue(0, "").trim()
        }

        flow.take(1).toList() shouldBeEqualTo listOf("Alice")
        flow.toList() shouldBeEqualTo listOf("Alice", "Bob", "Charlie")
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
