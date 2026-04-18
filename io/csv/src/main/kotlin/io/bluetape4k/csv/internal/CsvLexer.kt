package io.bluetape4k.csv.internal

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.logging.KLogging
import java.io.BufferedReader
import java.io.Closeable
import java.io.Reader

/**
 * RFC 4180 기반 CSV 렉서(Lexer).
 *
 * 5가지 상태([State.START_FIELD], [State.IN_QUOTED], [State.QUOTE_IN_QUOTED],
 * [State.IN_UNQUOTED], [State.END_ROW])를 이용한 문자 단위 상태 기계로
 * CSV 입력을 [ArrayRecord] 시퀀스로 변환합니다.
 *
 * ## 주요 특징
 * - **BOM 감지/제거**: [CsvSettings.detectBom]이 `true`이면 UTF-8 BOM을 자동 제거합니다.
 * - **Doubled-quote 이스케이프**: RFC 4180 표준에 따른 `""` → `"` 변환을 지원합니다.
 * - **CR/LF/CRLF 지원**: 세 가지 라인 구분자를 모두 인식합니다.
 * - **skipEmptyLines**: 물리적 빈 줄(컬럼이 하나도 없는 줄)만 건너뜁니다.
 * - **trimValues**: 인용되지 않은 필드의 앞뒤 공백을 제거합니다.
 * - **emptyValueAsNull / emptyQuotedAsNull**: 빈 필드/빈 인용 필드의 null 변환 정책.
 * - **maxCharsPerColumn / maxColumns**: 과도한 입력에 대한 방어 한계.
 *
 * ## 사용 예
 * ```kotlin
 * CsvLexer(reader, CsvSettings.DEFAULT, skipHeaders = true).use { lexer ->
 *     for (record in lexer) {
 *         println(record.rawValues.toList())
 *     }
 * }
 * ```
 *
 * @param reader 입력 Reader (내부적으로 [BufferedReader]로 감싸집니다)
 * @param settings CSV 설정
 * @param skipHeaders `true`이면 첫 번째 행을 헤더로 읽어 저장하고 이후 행부터 반환합니다.
 */
internal class CsvLexer(
    reader: Reader,
    private val settings: CsvSettings,
    private val skipHeaders: Boolean = false,
) : Iterator<ArrayRecord>, Closeable {

    companion object : KLogging() {
        /** BOM(Byte Order Mark) 문자 (U+FEFF). */
        private const val BOM_CHAR: Int = 0xFEFF
    }

    /**
     * 5가지 파싱 상태.
     */
    private enum class State {
        /** 필드 시작 대기 상태. */
        START_FIELD,

        /** 인용 필드 내부 (closing quote 대기). */
        IN_QUOTED,

        /** 인용 필드 내 quote 문자 발견 (doubled-quote 여부 판단 중). */
        QUOTE_IN_QUOTED,

        /** 비인용 필드 내부. */
        IN_UNQUOTED,

        /** 행 완료. */
        END_ROW,
    }

    // ────────────────────────────────────────
    // 설정값 캐시
    // ────────────────────────────────────────

    private val bufferedReader: BufferedReader
    private val delimiter: Char = settings.delimiter
    private val quote: Char = settings.quote
    private val quoteEscape: Char = settings.quoteEscape
    private val lineSeparator: String = settings.lineSeparator
    private val trimValues: Boolean = settings.trimValues
    private val skipEmptyLines: Boolean = settings.skipEmptyLines
    private val emptyValueAsNull: Boolean = settings.emptyValueAsNull
    private val emptyQuotedAsNull: Boolean = settings.emptyQuotedAsNull
    private val maxCharsPerColumn: Int = settings.maxCharsPerColumn
    private val maxColumns: Int = settings.maxColumns
    private val bufferSize: Int = settings.bufferSize

    // ────────────────────────────────────────
    // 헤더 관련
    // ────────────────────────────────────────

    private var headers: Array<String>? = null
    private var headerIndex: HeaderIndex? = null

    // ────────────────────────────────────────
    // 상태 변수
    // ────────────────────────────────────────

    /** 1-based 행 번호 (반환된 데이터 레코드 기준). */
    private var rowNumber: Long = 0L

    /** 현재 행 내 column 번호 (human-facing, 1-based). */
    private var columnNumber: Int = 0

    /** 다음에 반환할 레코드(prefetch). */
    private var nextRecord: ArrayRecord? = null

    /** 스트림이 모두 소진되었는지 여부. */
    private var exhausted: Boolean = false

    /** 필드 버퍼. */
    private val fieldBuffer: StringBuilder = StringBuilder(256)

    /** 현재 필드가 인용된 필드인지 여부. */
    private var lastFieldWasQuoted: Boolean = false

    /** 파싱 상태. */
    private var state: State = State.START_FIELD

    init {
        bufferedReader = createReader(reader)
        if (skipHeaders) {
            // 첫 번째 물리적 행을 헤더로 읽는다. 빈 줄은 건너뛰어 첫 비어 있지 않은 행을 헤더로 사용.
            while (true) {
                val firstRow = parseRow()
                if (firstRow == null) {
                    exhausted = true
                    break
                }
                if (firstRow.isEmpty()) {
                    // 물리적 빈 줄 — 다음 행으로 이동
                    continue
                }
                val arr = Array(firstRow.size) { i -> firstRow[i] ?: "" }
                headers = arr
                headerIndex = HeaderIndex.of(arr)
                break
            }
        }
    }

    // ────────────────────────────────────────
    // Iterator / Closeable
    // ────────────────────────────────────────

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
        runCatching { bufferedReader.close() }
    }

    // ────────────────────────────────────────
    // 내부 구현
    // ────────────────────────────────────────

    /**
     * 입력 Reader를 [BufferedReader]로 감싸고, [CsvSettings.detectBom]이 `true`이면
     * 선두의 BOM을 감지해 소비한다.
     */
    private fun createReader(reader: Reader): BufferedReader {
        val br = if (reader is BufferedReader) reader else reader.buffered(bufferSize)
        if (!settings.detectBom) return br
        br.mark(1)
        val firstChar = br.read()
        if (firstChar == BOM_CHAR) {
            // BOM 소비 완료
        } else if (firstChar != -1) {
            br.reset()
        }
        return br
    }

    /**
     * skipEmptyLines 정책을 적용하여 다음 유효한 레코드를 읽는다.
     */
    private fun readNextRecord(): ArrayRecord? {
        while (true) {
            val fields = parseRow()
            if (fields == null) {
                exhausted = true
                return null
            }
            // 물리적 빈 줄(컬럼 하나도 없음) 처리
            if (fields.isEmpty()) {
                if (skipEmptyLines) continue
                // skipEmptyLines=false일 때는 단일 null/empty 필드로 취급
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

    /**
     * 한 행을 파싱하여 필드 리스트를 반환한다.
     *
     * @return 파싱된 필드 리스트. EOF 시 `null`. 물리적 빈 줄이면 `emptyList()`.
     */
    private fun parseRow(): List<String?>? {
        val fields = mutableListOf<String?>()
        state = State.START_FIELD
        fieldBuffer.clear()
        lastFieldWasQuoted = false
        columnNumber = 0

        while (true) {
            val ch = bufferedReader.read()

            if (ch == -1) {
                // EOF 처리
                return handleEof(fields)
            }

            val c = ch.toChar()

            when (state) {
                State.START_FIELD -> {
                    when (c) {
                        quote -> {
                            lastFieldWasQuoted = true
                            state = State.IN_QUOTED
                        }

                        delimiter -> {
                            // 빈 필드
                            appendField(fields)
                        }

                        '\r' -> {
                            // CR 뒤 LF 여부 확인 후 행 종료
                            consumeLfAfterCr()
                            return finalizeRowAtStart(fields)
                        }

                        '\n' -> {
                            return finalizeRowAtStart(fields)
                        }

                        else -> {
                            state = State.IN_UNQUOTED
                            columnNumber++
                            appendCharToBuffer(c, fields.size)
                        }
                    }
                }

                State.IN_QUOTED -> {
                    when (c) {
                        quote -> state = State.QUOTE_IN_QUOTED
                        else -> {
                            appendCharToBuffer(c, fields.size)
                        }
                    }
                }

                State.QUOTE_IN_QUOTED -> {
                    when (c) {
                        // RFC 4180: quoteEscape == quote 이므로 doubled-quote → literal quote
                        quoteEscape -> {
                            fieldBuffer.append(quote)
                            state = State.IN_QUOTED
                        }

                        delimiter -> {
                            appendField(fields)
                            state = State.START_FIELD
                        }

                        '\r' -> {
                            consumeLfAfterCr()
                            appendField(fields)
                            return fields
                        }

                        '\n' -> {
                            appendField(fields)
                            return fields
                        }

                        else -> {
                            // closing quote 뒤 비구분자/비EOL — 관대하게 unquoted로 전환
                            state = State.IN_UNQUOTED
                            appendCharToBuffer(c, fields.size)
                        }
                    }
                }

                State.IN_UNQUOTED -> {
                    when (c) {
                        delimiter -> {
                            appendField(fields)
                            state = State.START_FIELD
                        }

                        '\r' -> {
                            consumeLfAfterCr()
                            appendField(fields)
                            return fields
                        }

                        '\n' -> {
                            appendField(fields)
                            return fields
                        }

                        else -> {
                            columnNumber++
                            appendCharToBuffer(c, fields.size)
                        }
                    }
                }

                State.END_ROW -> {
                    // 사용되지 않음(행은 return 으로 종료)
                    return fields
                }
            }
        }
    }

    /**
     * CR 다음 문자가 LF이면 소비하고, 아니면 reset으로 되돌린다.
     */
    private fun consumeLfAfterCr() {
        bufferedReader.mark(1)
        val next = bufferedReader.read()
        if (next != '\n'.code && next != -1) {
            bufferedReader.reset()
        }
    }

    /**
     * EOF 시점에서 남은 필드를 정리한다.
     *
     * - 아직 필드를 전혀 시작하지 않았고 버퍼도 비어 있으면 `null` 반환(진짜 EOF).
     * - 그 외에는 현재 필드를 마무리하여 반환.
     */
    private fun handleEof(fields: MutableList<String?>): List<String?>? {
        if (fields.isEmpty() && fieldBuffer.isEmpty() && state == State.START_FIELD && !lastFieldWasQuoted) {
            return null
        }
        fields.add(finishField())
        if (fields.size > maxColumns) {
            throw ParseException(
                message = "컬럼 수가 maxColumns($maxColumns)를 초과했습니다",
                rowNumber = rowNumber + 1,
                columnNumber = columnNumber,
                fieldIndex = fields.size - 1,
            )
        }
        return fields
    }

    /**
     * START_FIELD 상태에서 라인 종료를 만났을 때 행을 마무리한다.
     *
     * - 아무 필드도 없고 버퍼도 비어 있으면 물리적 빈 줄(`emptyList()`)로 반환.
     * - 그 외에는 현재 필드를 마무리하여 반환.
     */
    private fun finalizeRowAtStart(fields: MutableList<String?>): List<String?> {
        return if (fields.isEmpty() && fieldBuffer.isEmpty() && !lastFieldWasQuoted) {
            emptyList()
        } else {
            fields.add(finishField())
            if (fields.size > maxColumns) {
                throw ParseException(
                    message = "컬럼 수가 maxColumns($maxColumns)를 초과했습니다",
                    rowNumber = rowNumber + 1,
                    columnNumber = columnNumber,
                    fieldIndex = fields.size - 1,
                )
            }
            fields
        }
    }

    /**
     * 현재 버퍼의 필드를 확정하여 리스트에 추가하고, 다음 필드를 위해 상태를 초기화한다.
     * maxColumns 초과 시 [ParseException]을 던진다.
     */
    private fun appendField(fields: MutableList<String?>) {
        fields.add(finishField())
        if (fields.size > maxColumns) {
            throw ParseException(
                message = "컬럼 수가 maxColumns($maxColumns)를 초과했습니다",
                rowNumber = rowNumber + 1,
                columnNumber = columnNumber,
                fieldIndex = fields.size - 1,
            )
        }
        fieldBuffer.clear()
        lastFieldWasQuoted = false
    }

    /**
     * 버퍼에 문자를 추가하며 maxCharsPerColumn 한계를 체크한다.
     */
    private fun appendCharToBuffer(c: Char, currentFieldIndex: Int) {
        fieldBuffer.append(c)
        if (fieldBuffer.length > maxCharsPerColumn) {
            throw ParseException(
                message = "컬럼 크기가 maxCharsPerColumn($maxCharsPerColumn)을 초과했습니다",
                rowNumber = rowNumber + 1,
                columnNumber = columnNumber,
                fieldIndex = currentFieldIndex,
            )
        }
    }

    /**
     * 현재 버퍼의 내용을 필드 값으로 확정하고 반환한다.
     *
     * ## null/empty 분기
     * - **lastFieldWasQuoted=true, raw.isEmpty(), emptyQuotedAsNull=true** → `null`
     * - **lastFieldWasQuoted=true, raw.isEmpty(), emptyQuotedAsNull=false** → `""`
     * - **lastFieldWasQuoted=true, raw.isNotEmpty()** → `raw`
     * - **lastFieldWasQuoted=false, raw.isEmpty(), emptyValueAsNull=true** → `null`
     * - **lastFieldWasQuoted=false, raw.isEmpty(), emptyValueAsNull=false** → `""`
     * - **lastFieldWasQuoted=false, raw.isNotEmpty()** → trimValues 적용 후 `raw`
     */
    private fun finishField(): String? {
        val wasQuoted = lastFieldWasQuoted
        var raw = fieldBuffer.toString()
        fieldBuffer.clear()

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

    /**
     * 파싱된 헤더 이름 배열(skipHeaders=true일 때만 non-null).
     */
    fun headerNames(): Array<String>? = headers?.copyOf()

    /**
     * 헤더 인덱스 객체(skipHeaders=true일 때만 non-null).
     */
    fun headerIndex(): HeaderIndex? = headerIndex
}
