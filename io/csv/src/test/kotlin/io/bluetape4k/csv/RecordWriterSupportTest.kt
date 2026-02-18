package io.bluetape4k.csv

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.StringWriter

class RecordWriterSupportTest {

    companion object: KLogging()

    @TempDir
    lateinit var tempDir: File

    data class Person(val name: String, val age: Int, val city: String)

    private val people = listOf(
        Person("Alice", 20, "Seoul"),
        Person("Bob", 30, "Busan"),
        Person("Charlie", 25, "Daegu"),
    )

    // region File 확장 함수 테스트

    @Test
    fun `File에 CSV 레코드를 기록한다`() {
        val csvFile = File(tempDir, "output.csv")

        csvFile.writeCsvRecords(
            headers = listOf("name", "age", "city"),
            rows = listOf(
                listOf("Alice", 20, "Seoul"),
                listOf("Bob", 30, "Busan"),
            )
        )

        val content = csvFile.readText()
        log.debug { "content=\n$content" }
        content shouldContain "name,age,city"
        content shouldContain "Alice,20,Seoul"
        content shouldContain "Bob,30,Busan"
    }

    @Test
    fun `File에 CSV 레코드를 헤더 없이 기록한다`() {
        val csvFile = File(tempDir, "no_header.csv")

        csvFile.writeCsvRecords(
            rows = listOf(
                listOf("Alice", 20, "Seoul"),
                listOf("Bob", 30, "Busan"),
            )
        )

        val content = csvFile.readText()
        log.debug { "content=\n$content" }
        content shouldContain "Alice,20,Seoul"
        content shouldContain "Bob,30,Busan"
    }

    @Test
    fun `File에 엔티티를 CSV 레코드로 변환하여 기록한다`() {
        val csvFile = File(tempDir, "entities.csv")

        csvFile.writeCsvRecords(
            headers = listOf("name", "age", "city"),
            entities = people,
        ) { person -> listOf(person.name, person.age, person.city) }

        val content = csvFile.readText()
        log.debug { "content=\n$content" }
        content shouldContain "name,age,city"
        content shouldContain "Alice,20,Seoul"
        content shouldContain "Charlie,25,Daegu"
    }

    @Test
    fun `File에 TSV 레코드를 기록한다`() {
        val tsvFile = File(tempDir, "output.tsv")

        tsvFile.writeTsvRecords(
            headers = listOf("name", "age", "city"),
            rows = listOf(
                listOf("Alice", 20, "Seoul"),
                listOf("Bob", 30, "Busan"),
            )
        )

        val content = tsvFile.readText()
        log.debug { "content=\n$content" }
        content shouldContain "name\tage\tcity"
        content shouldContain "Alice\t20\tSeoul"
        content shouldContain "Bob\t30\tBusan"
    }

    @Test
    fun `File에 엔티티를 TSV 레코드로 변환하여 기록한다`() {
        val tsvFile = File(tempDir, "entities.tsv")

        tsvFile.writeTsvRecords(
            headers = listOf("name", "age", "city"),
            entities = people,
        ) { person -> listOf(person.name, person.age, person.city) }

        val content = tsvFile.readText()
        log.debug { "content=\n$content" }
        content shouldContain "name\tage\tcity"
        content shouldContain "Alice\t20\tSeoul"
        content shouldContain "Charlie\t25\tDaegu"
    }

    // endregion

    // region File 라운드트립 테스트

    @Test
    fun `CSV 파일에 기록하고 다시 읽는다`() {
        val csvFile = File(tempDir, "roundtrip.csv")

        csvFile.writeCsvRecords(
            headers = listOf("name", "age", "city"),
            entities = people,
        ) { person -> listOf(person.name, person.age, person.city) }

        val records = csvFile.readAsCsvRecords(skipHeader = true).toList()
        records.shouldNotBeEmpty()

        records.forEach { record ->
            log.trace { "record=${record.values.toList()}" }
        }
    }

    @Test
    fun `TSV 파일에 기록하고 다시 읽는다`() {
        val tsvFile = File(tempDir, "roundtrip.tsv")

        tsvFile.writeTsvRecords(
            headers = listOf("name", "age", "city"),
            entities = people,
        ) { person -> listOf(person.name, person.age, person.city) }

        val records = tsvFile.readAsTsvRecords(skipHeader = true).toList()
        records.shouldNotBeEmpty()

        records.forEach { record ->
            log.debug { "record=${record.values.toList()}" }
        }
    }

    // endregion

    // region writeAll(Iterable) 테스트

    @Test
    fun `writeAll로 Iterable 데이터를 기록한다`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                val rows = listOf(
                    listOf("row1", 1, 2),
                    listOf("row2", 3, 4),
                )
                writer.writeAll(rows)
            }

            val captured = sw.buffer.toString()
            log.debug { "captured=\n$captured" }
            captured shouldContain "row1,1,2"
            captured shouldContain "row2,3,4"
        }
    }

    @Test
    fun `writeAll로 Iterable 엔티티를 변환하여 기록한다`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("name", "age", "city")
                writer.writeAll(people) { person ->
                    listOf(person.name, person.age, person.city)
                }
            }

            val captured = sw.buffer.toString()
            log.debug { "captured=\n$captured" }
            captured shouldContain "name,age,city"
            captured shouldContain "Alice,20,Seoul"
            captured shouldContain "Bob,30,Busan"
            captured shouldContain "Charlie,25,Daegu"
        }
    }

    @Test
    fun `writeRow로 entity를 변환하여 기록한다`() {
        StringWriter().use { sw ->
            CsvRecordWriter(sw).use { writer ->
                writer.writeHeaders("name", "age", "city")
                people.forEach { person ->
                    writer.writeRow(person) { p -> listOf(p.name, p.age, p.city) }
                }
            }

            val captured = sw.buffer.toString()
            log.debug { "captured=\n$captured" }
            captured shouldContain "Alice,20,Seoul"
            captured shouldContain "Charlie,25,Daegu"
        }
    }

    // endregion
}
