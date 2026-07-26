package io.bluetape4k.csv.internal

import okio.BufferedSink

/**
 * Segment-backed CSV/TSV writer for UTF-8 file output.
 *
 * This writer preserves [DelimitedWriter] field semantics while avoiding the
 * `Writer.write(...)` character path for the common UTF-8 export pipeline.
 */
internal class OkioDelimitedWriter(
    private val sink: BufferedSink,
    private val delimiter: Char,
    private val quote: Char,
    private val quoteEscape: Char,
    private val lineSeparator: String,
) {

    /**
     * Writes one row with selective quoting.
     */
    fun writeRow(fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) sink.writeUtf8Char(delimiter)
            first = false

            when (field) {
                null -> { /* unquoted empty field */
                }
                is String -> {
                    if (field.isEmpty()) {
                        writeQuoted(field)
                    } else if (needsQuoting(field)) {
                        writeQuoted(field)
                    } else {
                        sink.writeUtf8(field)
                    }
                }
                else -> {
                    val str = field.toString()
                    if (needsQuoting(str)) writeQuoted(str) else sink.writeUtf8(str)
                }
            }
        }
        sink.writeUtf8(lineSeparator)
    }

    /**
     * Writes one row while quoting every non-null field.
     */
    fun writeAllQuoted(fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) sink.writeUtf8Char(delimiter)
            first = false

            if (field != null) {
                val str = if (field is String) field else field.toString()
                writeQuoted(str)
            }
        }
        sink.writeUtf8(lineSeparator)
    }

    private fun needsQuoting(s: String): Boolean {
        if (s.isEmpty()) return false
        if (s[0] == ' ' || s[s.length - 1] == ' ') return true
        for (c in s) {
            if (c == delimiter || c == quote || c == '\r' || c == '\n') return true
        }
        return false
    }

    private fun writeQuoted(s: String) {
        sink.writeUtf8Char(quote)
        var start = 0
        for (index in s.indices) {
            if (s[index] == quote) {
                if (start < index) {
                    sink.writeUtf8(s, start, index)
                }
                sink.writeUtf8Char(quoteEscape)
                sink.writeUtf8Char(quote)
                start = index + 1
            }
        }
        if (start < s.length) {
            sink.writeUtf8(s, start, s.length)
        }
        sink.writeUtf8Char(quote)
    }

    private fun BufferedSink.writeUtf8Char(c: Char) {
        if (c.code <= 0x7F) {
            writeByte(c.code)
        } else {
            writeUtf8CodePoint(c.code)
        }
    }
}
