package io.bluetape4k.csv.v2

import io.bluetape4k.csv.internal.DelimitedWriter
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path

internal class FlowCsvWriterImpl(
    private val writer: Writer,
    override val config: CsvWriterConfig,
) : FlowCsvWriter {

    companion object : KLogging()

    private val settings = config.toCsvSettings()
    private val delimiter = settings.delimiter
    private val quote = settings.quote
    private val lineSeparator = settings.lineSeparator

    private val delimitedWriter = DelimitedWriter(
        writer = writer,
        delimiter = delimiter,
        quote = quote,
        quoteEscape = settings.quoteEscape,
        lineSeparator = lineSeparator,
    )
    private val mutex = Mutex()

    override suspend fun writeHeaders(headers: Iterable<String>) {
        mutex.withLock {
            writeRowTo(writer, headers)
        }
    }

    override suspend fun writeRow(row: Iterable<*>) {
        mutex.withLock {
            writeRowTo(writer, row)
        }
    }

    override suspend fun writeAll(rows: Flow<Iterable<*>>) {
        rows.collect { writeRow(it) }
    }

    override suspend fun writeFile(
        path: Path,
        encoding: Charset,
        append: Boolean,
        skipHeaders: Boolean,
        headers: List<String>,
        rows: Flow<Iterable<*>>,
    ): Long {
        var count = 0L
        OutputStreamWriter(FileOutputStream(path.toFile(), append), encoding).use { fw ->
            // 행마다 DelimitedWriter 재생성을 피하기 위해 파일 전용 인스턴스 1개 생성
            val fileWriter = DelimitedWriter(fw, delimiter, quote, settings.quoteEscape, lineSeparator)
            if (!skipHeaders && headers.isNotEmpty()) {
                writeRowToDelimited(fileWriter, fw, headers)
            }
            rows.collect { row ->
                writeRowToDelimited(fileWriter, fw, row)
                count++
            }
        }
        return count
    }

    override fun close() {
        runCatching {
            writer.flush()
            delimitedWriter.close()
            writer.close()
        }
    }

    private fun writeRowTo(w: Writer, fields: Iterable<*>) {
        if (config.quoteAll) {
            writeAllQuoted(w, fields)
        } else {
            delimitedWriter.writeRow(fields)
        }
    }

    private fun writeRowToDelimited(dw: DelimitedWriter, w: Writer, fields: Iterable<*>) {
        if (config.quoteAll) {
            writeAllQuoted(w, fields)
        } else {
            dw.writeRow(fields)
        }
    }

    private fun writeAllQuoted(w: Writer, fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) w.write(delimiter.code)
            first = false
            when (field) {
                null -> { /* 인용 없는 빈 필드 */ }
                else -> {
                    val s = if (field is String) field else field.toString()
                    w.write(quote.code)
                    for (c in s) {
                        if (c == quote) {
                            w.write(quote.code)  // RFC 4180 doubled-quote
                            w.write(quote.code)
                        } else {
                            w.write(c.code)
                        }
                    }
                    w.write(quote.code)
                }
            }
        }
        w.write(lineSeparator)
    }
}
