package io.bluetape4k.csv.coroutines

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.csv.DefaultTsvWriterSettings
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.Flow
import java.io.Writer

/**
 * TSV 포맷으로 데이터를 파일로 쓰는 [SuspendRecordWriter] 입니다.
 *
 * ```
 * val writer = CoTsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer TSV writer
 */
class SuspendTsvRecordWriter private constructor(
    private val writer: TsvWriter,
): SuspendRecordWriter {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun invoke(writer: TsvWriter): SuspendTsvRecordWriter {
            return SuspendTsvRecordWriter(writer)
        }

        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: TsvWriterSettings = DefaultTsvWriterSettings,
        ): SuspendTsvRecordWriter {
            return invoke(TsvWriter(writer, settings))
        }
    }

    override suspend fun writeHeaders(headers: Iterable<String>) {
        writer.writeHeaders(headers.toFastList())
    }

    override suspend fun writeRow(row: Iterable<*>) {
        writer.writeRow(row.toFastList())
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
