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
        /**
         * CSV/TSV 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(csvWriter: CsvWriter): CsvRecordWriter {
            return CsvRecordWriter(csvWriter)
        }

        /**
         * CSV/TSV 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): CsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeHeaders` 함수를 제공합니다.
     */
    override fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeRow` 함수를 제공합니다.
     */
    override fun writeRow(rows: Iterable<*>) {
        writer.writeRow(rows.toFastList())
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeAll` 함수를 제공합니다.
     */
    override fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runCatching { writer.close() }
    }
}
