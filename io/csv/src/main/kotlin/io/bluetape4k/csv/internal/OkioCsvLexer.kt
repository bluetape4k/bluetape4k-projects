package io.bluetape4k.csv.internal

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import java.io.Closeable

/**
 * UTF-8 CSV lexer backed by Okio segments.
 *
 * This lexer keeps the public CSV semantics of [CsvLexer] for UTF-8 input with
 * single-byte ASCII delimiter and quote characters. It reads structural bytes
 * from Okio segments and decodes field payloads only when a field is complete.
 */
internal class OkioCsvLexer(
    private val source: BufferedSource,
    private val settings: CsvSettings,
    private val skipHeaders: Boolean = false,
): Iterator<ArrayRecord>, Closeable {

    companion object: KLogging() {
        private const val BOM_1: Byte = 0xEF.toByte()
        private const val BOM_2: Byte = 0xBB.toByte()
        private const val BOM_3: Byte = 0xBF.toByte()
        private const val CR: Byte = '\r'.code.toByte()
        private const val LF: Byte = '\n'.code.toByte()

        fun isSupported(settings: CsvSettings): Boolean =
            settings.delimiter.code in 0x00..0x7F &&
                    settings.quote.code in 0x00..0x7F &&
                    settings.delimiter != '\r' &&
                    settings.delimiter != '\n' &&
                    settings.quote != '\r' &&
                    settings.quote != '\n' &&
                    settings.quoteEscape == settings.quote
    }

    private enum class State {
        START_FIELD,
        IN_QUOTED,
        QUOTE_IN_QUOTED,
        IN_UNQUOTED,
    }

    private val delimiter: Byte = settings.delimiter.code.toByte()
    private val quote: Byte = settings.quote.code.toByte()
    private val unquotedTerminators: ByteString = ByteString.of(delimiter, CR, LF)
    private val quoteTerminators: ByteString = ByteString.of(quote)
    private val trimValues: Boolean = settings.trimValues
    private val skipEmptyLines: Boolean = settings.skipEmptyLines
    private val emptyValueAsNull: Boolean = settings.emptyValueAsNull
    private val emptyQuotedAsNull: Boolean = settings.emptyQuotedAsNull
    private val maxCharsPerColumn: Int = settings.maxCharsPerColumn
    private val maxFieldBytes: Long = maxCharsPerColumn.toLong() * 4L
    private val maxColumns: Int = settings.maxColumns
    private val fieldBuffer = Buffer()

    private var headers: Array<String>? = null
    private var headerIndex: HeaderIndex? = null
    private var rowNumber: Long = 0L
    private var columnNumber: Int = 0
    private var nextRecord: ArrayRecord? = null
    private var exhausted: Boolean = false
    private var lastFieldWasQuoted: Boolean = false
    private var state: State = State.START_FIELD

    init {
        require(isSupported(settings)) {
            "OkioCsvLexer requires single-byte ASCII delimiter/quote and doubled-quote escaping."
        }
        consumeBomIfPresent()
        if (skipHeaders) {
            while (true) {
                val firstRow = parseRow()
                if (firstRow == null) {
                    exhausted = true
                    break
                }
                if (firstRow.isEmpty()) continue
                val arr = Array(firstRow.size) { i -> firstRow[i] ?: "" }
                headers = arr
                headerIndex = HeaderIndex.of(arr)
                break
            }
        }
    }

    override fun hasNext(): Boolean {
        if (exhausted) return nextRecord != null
        if (nextRecord != null) return true
        nextRecord = readNextRecord()
        return nextRecord != null
    }

    override fun next(): ArrayRecord {
        if (!hasNext()) throw NoSuchElementException("더 이상 읽을 레코드가 없습니다")
        val record = nextRecord!!
        nextRecord = null
        return record
    }

    override fun close() {
        runCatching { source.close() }
    }

    private fun consumeBomIfPresent() {
        if (!settings.detectBom) return
        if (source.request(3) &&
            source.buffer[0] == BOM_1 &&
            source.buffer[1] == BOM_2 &&
            source.buffer[2] == BOM_3
        ) {
            source.skip(3)
        }
    }

    private fun readNextRecord(): ArrayRecord? {
        while (true) {
            val fields = parseRow()
            if (fields == null) {
                exhausted = true
                return null
            }
            if (fields.isEmpty()) {
                if (skipEmptyLines) continue
                rowNumber++
                return ArrayRecord(
                    rawValues = arrayOfNulls(1),
                    _headers = headers,
                    headerIndex = headerIndex,
                    rowNumber = rowNumber,
                )
            }
            rowNumber++
            return ArrayRecord(
                rawValues = fields.toTypedArray(),
                _headers = headers,
                headerIndex = headerIndex,
                rowNumber = rowNumber,
            )
        }
    }

    private fun parseRow(): List<String?>? {
        val fields = ArrayList<String?>(16)
        state = State.START_FIELD
        fieldBuffer.clear()
        lastFieldWasQuoted = false
        columnNumber = 0

        while (true) {
            when (state) {
                State.START_FIELD     -> {
                    if (source.exhausted()) return handleEof(fields)
                    when (val b = source.readByte()) {
                        quote -> {
                            lastFieldWasQuoted = true
                            state = State.IN_QUOTED
                        }
                        delimiter -> appendField(fields)
                        CR    -> {
                            consumeLfAfterCr()
                            return finalizeRowAtStart(fields)
                        }
                        LF    -> return finalizeRowAtStart(fields)
                        else  -> {
                            state = State.IN_UNQUOTED
                            columnNumber++
                            appendByteToBuffer(b, fields.size)
                        }
                    }
                }

                State.IN_QUOTED       -> {
                    val terminator = transferUntil(fields, quoteTerminators)
                    if (terminator == null) return handleEof(fields)
                    state = State.QUOTE_IN_QUOTED
                }

                State.QUOTE_IN_QUOTED -> {
                    if (source.exhausted()) {
                        appendField(fields)
                        return fields
                    }
                    when (val b = source.readByte()) {
                        quote -> {
                            appendByteToBuffer(quote, fields.size)
                            state = State.IN_QUOTED
                        }
                        delimiter -> {
                            appendField(fields)
                            state = State.START_FIELD
                        }
                        CR    -> {
                            consumeLfAfterCr()
                            appendField(fields)
                            return fields
                        }
                        LF    -> {
                            appendField(fields)
                            return fields
                        }
                        else  -> {
                            state = State.IN_UNQUOTED
                            appendByteToBuffer(b, fields.size)
                        }
                    }
                }

                State.IN_UNQUOTED     -> {
                    val terminator = transferUntil(fields, unquotedTerminators)
                    if (terminator == null) return handleEof(fields)
                    when (terminator) {
                        delimiter -> {
                            appendField(fields)
                            state = State.START_FIELD
                        }
                        CR -> {
                            consumeLfAfterCr()
                            appendField(fields)
                            return fields
                        }
                        LF -> {
                            appendField(fields)
                            return fields
                        }
                    }
                }
            }
        }
    }

    private fun transferUntil(fields: List<String?>, terminators: ByteString): Byte? {
        while (source.request(1)) {
            val offset = findTerminatorOffset(terminators)
            if (offset >= 0L) {
                if (offset > 0L) {
                    fieldBuffer.write(source.buffer, offset)
                    checkFieldByteBound(fields.size)
                }
                return source.readByte()
            }

            val bufferedSize = source.buffer.size
            if (fieldBuffer.size + bufferedSize > maxFieldBytes) {
                throwColumnTooLarge(fields.size)
            }
            fieldBuffer.write(source.buffer, bufferedSize)
        }
        return null
    }

    private fun findTerminatorOffset(terminators: ByteString): Long {
        source.buffer.readUnsafe().use { cursor ->
            while (cursor.next() != -1) {
                val data = cursor.data ?: continue
                var index = cursor.start
                while (index < cursor.end) {
                    if (isTerminator(data[index], terminators)) {
                        return cursor.offset + index - cursor.start
                    }
                    index++
                }
            }
        }
        return -1L
    }

    private fun isTerminator(byte: Byte, terminators: ByteString): Boolean =
        if (terminators == quoteTerminators) {
            byte == quote
        } else {
            byte == delimiter || byte == CR || byte == LF
        }

    private fun consumeLfAfterCr() {
        if (source.request(1) && source.buffer[0] == LF) {
            source.skip(1)
        }
    }

    private fun handleEof(fields: MutableList<String?>): List<String?>? {
        if (fields.isEmpty() && fieldBuffer.size == 0L && state == State.START_FIELD && !lastFieldWasQuoted) {
            return null
        }
        fields.add(finishField(fields.size))
        checkMaxColumns(fields)
        return fields
    }

    private fun finalizeRowAtStart(fields: MutableList<String?>): List<String?> {
        return if (fields.isEmpty() && fieldBuffer.size == 0L && !lastFieldWasQuoted) {
            emptyList()
        } else {
            fields.add(finishField(fields.size))
            checkMaxColumns(fields)
            fields
        }
    }

    private fun appendField(fields: MutableList<String?>) {
        fields.add(finishField(fields.size))
        checkMaxColumns(fields)
        fieldBuffer.clear()
        lastFieldWasQuoted = false
    }

    private fun appendByteToBuffer(byte: Byte, currentFieldIndex: Int) {
        fieldBuffer.writeByte(byte.toInt())
        checkFieldByteBound(currentFieldIndex)
    }

    private fun checkFieldByteBound(currentFieldIndex: Int) {
        if (fieldBuffer.size > maxFieldBytes) {
            throwColumnTooLarge(currentFieldIndex)
        }
    }

    private fun checkMaxColumns(fields: List<String?>) {
        if (fields.size > maxColumns) {
            throw ParseException(
                message = "컬럼 수가 maxColumns($maxColumns)를 초과했습니다",
                rowNumber = rowNumber + 1,
                columnNumber = columnNumber,
                fieldIndex = fields.size - 1,
            )
        }
    }

    private fun throwColumnTooLarge(currentFieldIndex: Int): Nothing =
        throw ParseException(
            message = "컬럼 크기가 maxCharsPerColumn($maxCharsPerColumn)을 초과했습니다",
            rowNumber = rowNumber + 1,
            columnNumber = columnNumber,
            fieldIndex = currentFieldIndex,
        )

    private fun finishField(currentFieldIndex: Int): String? {
        val wasQuoted = lastFieldWasQuoted
        var raw = fieldBuffer.readString(Charsets.UTF_8)
        if (raw.length > maxCharsPerColumn) {
            throwColumnTooLarge(currentFieldIndex)
        }
        if (trimValues && !wasQuoted) {
            raw = raw.trim()
        }

        return if (wasQuoted) {
            lastFieldWasQuoted = false
            when {
                raw.isEmpty() && emptyQuotedAsNull -> null
                else -> raw
            }
        } else {
            when {
                raw.isEmpty() && emptyValueAsNull -> null
                else -> raw
            }
        }
    }

    fun headerNames(): Array<String>? = headers?.copyOf()

    fun headerIndex(): HeaderIndex? = headerIndex
}
