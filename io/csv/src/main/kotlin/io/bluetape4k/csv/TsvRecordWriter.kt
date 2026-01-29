package io.bluetape4k.csv

import com.univocity.parsers.tsv.TsvWriter
import com.univocity.parsers.tsv.TsvWriterSettings
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import java.io.Writer

/**
 * TSV 포맷으로 데이터를 파일로 쓰는 [RecordWriter] 입니다.
 *
 * ```
 * val writer = TsvRecordWriter(output)
 * writer.writeHeaders(listOf("name", "age"))
 * writer.writeRow(listOf("Alice", 20))
 * writer.writeRow(listOf("Bob", 30))
 * writer.close()
 * ```
 *
 * @property writer TSV writer
 */
class TsvRecordWriter private constructor(
    private val writer: TsvWriter,
): RecordWriter {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(tsvWriter: TsvWriter): TsvRecordWriter {
            return TsvRecordWriter(tsvWriter)
        }

        @JvmStatic
        operator fun invoke(
            writer: Writer,
            settings: TsvWriterSettings = DefaultTsvWriterSettings,
        ): TsvRecordWriter {
            return invoke(TsvWriter(writer, settings))
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
