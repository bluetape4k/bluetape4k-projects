package io.bluetape4k.csv.coroutines

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.csv.DefaultTsvWriterSettings
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import java.io.Writer

/**
 * TSV 포맷으로 데이터를 파일로 쓰는 [CoRecordWriter] 입니다.
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
class CoTsvRecordWriter private constructor(
    private val writer: TsvWriter,
): CoRecordWriter {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(writer: TsvWriter): CoTsvRecordWriter {
            return CoTsvRecordWriter(writer)
        }

        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: TsvWriterSettings = DefaultTsvWriterSettings,
        ): CoTsvRecordWriter {
            return invoke(TsvWriter(writer, settings))
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
        rows.buffer().collect { writeRow(it) }
    }

    override fun close() {
        runCatching { writer.close() }
    }
}
