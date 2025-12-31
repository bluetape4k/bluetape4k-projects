package io.bluetape4k.csv.coroutines

import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.bluetape4k.csv.DefaultCsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * CSV 포맷으로 데이터를 파일로 쓰는 [CoRecordWriter] 입니다.
 *
 * ```
 * val writer = CoCsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer CSV writer
 */
class SuspendCsvRecordWriter private constructor(
    private val writer: CsvWriter,
): SuspendRecordWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun invoke(writer: CsvWriter): SuspendCsvRecordWriter {
            return SuspendCsvRecordWriter(writer)
        }

        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): SuspendCsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toList())
    }

    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toList())
    }

    override suspend fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    override suspend fun writeAll(rows: Flow<Iterable<*>>) {
        rows.collect { writeRow(it) }
    }

    override fun close() {
        runCatching { writer.close() }
    }
}
