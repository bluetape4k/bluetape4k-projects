package io.bluetape4k.csv.v2

import io.bluetape4k.csv.internal.DelimitedWriter
import io.bluetape4k.csv.internal.OkioDelimitedWriter
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8

internal class FlowCsvWriterImpl(
    private val writer: Writer,
    override val config: CsvWriterConfig,
): FlowCsvWriter {

    companion object: KLogging()

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
            withContext(Dispatchers.IO) { writeRowTo(writer, headers) }
        }
    }

    override suspend fun writeRow(row: Iterable<*>) {
        mutex.withLock {
            withContext(Dispatchers.IO) { writeRowTo(writer, row) }
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
        return withContext(Dispatchers.IO) {
            if (encoding == UTF_8) {
                return@withContext writeUtf8FileWithOkio(path, append, skipHeaders, headers, rows)
            }
            writeFileWithWriter(path, encoding, append, skipHeaders, headers, rows)
        }
    }

    private suspend fun writeUtf8FileWithOkio(
        path: Path,
        append: Boolean,
        skipHeaders: Boolean,
        headers: List<String>,
        rows: Flow<Iterable<*>>,
    ): Long {
        var count = 0L
        FileOutputStream(path.toFile(), append).sink().buffer().use { sink ->
            val fileWriter = OkioDelimitedWriter(sink, delimiter, quote, settings.quoteEscape, lineSeparator)
            if (!skipHeaders && headers.isNotEmpty()) {
                writeRowToOkio(fileWriter, headers)
            }
            rows.collect { row ->
                currentCoroutineContext().ensureActive()
                writeRowToOkio(fileWriter, row)
                count++
            }
        }
        return count
    }

    private suspend fun writeFileWithWriter(
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
                currentCoroutineContext().ensureActive()
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
        }.onFailure { e -> log.warn(e) { "Failed to close CSV writer" } }
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

    private fun writeRowToOkio(w: OkioDelimitedWriter, fields: Iterable<*>) {
        if (config.quoteAll) {
            w.writeAllQuoted(fields)
        } else {
            w.writeRow(fields)
        }
    }

    private fun writeAllQuoted(w: Writer, fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) w.write(delimiter.code)
            first = false
            when (field) {
                null -> { /* 인용 없는 빈 필드 */
                }
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
