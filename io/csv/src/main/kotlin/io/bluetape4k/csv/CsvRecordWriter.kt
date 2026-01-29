package io.bluetape4k.csv

import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * CSV 포맷으로 데이터를 출력하는 Writer 입니다.
 *
 * ```
 * val writer = CsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer CSV writer
 */
class CsvRecordWriter private constructor(
    private val writer: CsvWriter,
): RecordWriter {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(csvWriter: CsvWriter): CsvRecordWriter {
            return CsvRecordWriter(csvWriter)
        }

        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): CsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    override fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    override fun writeRow(rows: Iterable<*>) {
        writer.writeRow(rows.toFastList())
    }

    override fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    override fun close() {
        runCatching { writer.close() }
    }
}
