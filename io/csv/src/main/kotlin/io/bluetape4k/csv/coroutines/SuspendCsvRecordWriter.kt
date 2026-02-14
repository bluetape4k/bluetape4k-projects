package io.bluetape4k.csv.coroutines

import com.univocity.parsers.csv.CsvWriter
import com.univocity.parsers.csv.CsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.csv.DefaultCsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * CSV 포맷으로 데이터를 파일로 쓰는 [SuspendRecordWriter] 입니다.
 *
 * ```
 * val writer = SuspendCsvRecordWriter(output)
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
        /**
         * CSV/TSV 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(writer: CsvWriter): SuspendCsvRecordWriter {
            return SuspendCsvRecordWriter(writer)
        }

        /**
         * CSV/TSV 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: CsvWriterSettings = DefaultCsvWriterSettings,
        ): SuspendCsvRecordWriter {
            return invoke(CsvWriter(writer, settings))
        }
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeHeaders` 함수를 제공합니다.
     */
    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeRow` 함수를 제공합니다.
     */
    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toFastList())
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeAll` 함수를 제공합니다.
     */
    override suspend fun writeAll(rows: Sequence<Iterable<*>>) {
        rows.forEach { writeRow(it) }
    }

    /**
     * CSV/TSV 처리에서 데이터를 기록하는 `writeAll` 함수를 제공합니다.
     */
    override suspend fun writeAll(rows: Flow<Iterable<*>>) {
        rows.collect { writeRow(it) }
    }

    /**
     * CSV/TSV 처리 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        runCatching { writer.close() }
    }
}
