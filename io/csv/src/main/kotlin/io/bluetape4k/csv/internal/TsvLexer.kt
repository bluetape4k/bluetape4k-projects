package io.bluetape4k.csv.internal

import io.bluetape4k.csv.TsvSettings
import io.bluetape4k.logging.KLogging
import java.io.BufferedReader
import java.io.Closeable
import java.io.Reader

/**
 * TSV 형식 파서. 탭(`\t`) 구분, 백슬래시 이스케이프 방식.
 * 인용(quote) 문자 없음.
 *
 * ## 이스케이프 규칙
 * - `\t` → 탭 문자
 * - `\n` → 개행 문자
 * - `\r` → CR 문자
 * - `\\` → 리터럴 백슬래시
 * - 그 외 `\X` → 리터럴 `\X` (관대한 처리)
 *
 * ## 사용 예
 * ```kotlin
 * val reader = File("data.tsv").bufferedReader()
 * val lexer = TsvLexer(reader, TsvSettings(), skipHeaders = true)
 * for (record in lexer) {
 *     println(record.getString("name"))
 * }
 * lexer.close()
 * ```
 *
 * @param reader 입력 Reader
 * @param settings TSV 파싱 설정 ([TsvSettings])
 * @param skipHeaders `true`이면 첫 번째 행을 헤더로 읽어 [HeaderIndex]를 구성한다
 */
internal class TsvLexer(
    reader: Reader,
    private val settings: TsvSettings,
    private val skipHeaders: Boolean = false,
) : Iterator<ArrayRecord>, Closeable {

    companion object : KLogging()

    /** 파서 내부 상태. */
    private enum class State {
        /** 탭 또는 개행 대기 중인 일반 필드 읽기 상태. */
        START_FIELD,

        /** 백슬래시 다음 문자를 처리하는 이스케이프 상태. */
        ESCAPE,
    }

    private val bufferedReader: BufferedReader =
        if (reader is BufferedReader) reader else reader.buffered(settings.bufferSize)

    private var headers: Array<String>? = null
    private var headerIndex: HeaderIndex? = null

    private var rowNumber: Long = 0L
    private var fieldIndex: Int = 0

    private val fieldBuffer = StringBuilder(256)
    private var nextRecord: ArrayRecord? = null
    private var exhausted = false

    init {
        if (skipHeaders) {
            val firstRow = parseRow()
            if (firstRow != null && firstRow.isNotEmpty()) {
                headers = firstRow.map { it ?: "" }.toTypedArray()
                headerIndex = HeaderIndex.of(headers!!)
            }
        }
    }

    /**
     * 다음 레코드가 존재하는지 확인한다.
     */
    override fun hasNext(): Boolean {
        if (exhausted) return false
        if (nextRecord != null) return true
        nextRecord = readNextRecord()
        return nextRecord != null
    }

    /**
     * 다음 레코드를 반환한다.
     * @throws NoSuchElementException 레코드가 없을 때
     */
    override fun next(): ArrayRecord {
        if (!hasNext()) throw NoSuchElementException("더 이상 레코드가 없습니다")
        val record = nextRecord!!
        nextRecord = null
        return record
    }

    /**
     * 다음 비어 있지 않은 레코드를 파싱하여 반환한다.
     * 입력이 소진되면 null을 반환하고 [exhausted]를 true로 설정한다.
     */
    private fun readNextRecord(): ArrayRecord? {
        while (true) {
            val fields = parseRow() ?: run { exhausted = true; return null }
            if (settings.skipEmptyLines && fields.isEmpty()) continue
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
     * 입력 스트림에서 한 행(레코드)을 파싱하여 필드 목록으로 반환한다.
     *
     * - EOF에서 버퍼가 비어 있으면 null 반환 (스트림 종료)
     * - 빈 줄(`\n` 직후 `\n`)이면 빈 리스트 반환 ([settings.skipEmptyLines]에서 필터링)
     * - CR-LF(`\r\n`) 시퀀스는 단일 레코드 구분자로 처리한다
     */
    private fun parseRow(): List<String?>? {
        val fields = mutableListOf<String?>()
        fieldBuffer.clear()
        var state = State.START_FIELD
        fieldIndex = 0

        while (true) {
            val ch = bufferedReader.read()

            when {
                // EOF 처리
                ch == -1 -> {
                    return if (fields.isEmpty() && fieldBuffer.isEmpty()) null
                    else {
                        fields.add(finishField())
                        fields
                    }
                }

                state == State.START_FIELD -> when (ch.toChar()) {
                    // 백슬래시 → 이스케이프 상태 전환
                    '\\' -> state = State.ESCAPE

                    // 탭 → 필드 구분
                    '\t' -> {
                        fields.add(finishField())
                        fieldIndex++
                        if (fields.size > settings.maxColumns) {
                            throw ParseException(
                                "컬럼 수가 maxColumns(${settings.maxColumns})를 초과했습니다",
                                rowNumber + 1,
                                fieldIndex,
                                fields.size - 1,
                            )
                        }
                    }

                    // CR: CR-LF 처리 — LF가 이어지면 소비, 아니면 되돌림
                    '\r' -> {
                        bufferedReader.mark(1)
                        val next = bufferedReader.read()
                        if (next != '\n'.code && next != -1) {
                            bufferedReader.reset()
                        }
                        fields.add(finishField())
                        return fields
                    }

                    // LF → 레코드 종료
                    '\n' -> {
                        fields.add(finishField())
                        return fields
                    }

                    // 일반 문자 → 버퍼에 추가
                    else -> {
                        fieldBuffer.append(ch.toChar())
                        if (fieldBuffer.length > settings.maxCharsPerColumn) {
                            throw ParseException(
                                "컬럼 크기가 maxCharsPerColumn(${settings.maxCharsPerColumn})을 초과했습니다",
                                rowNumber + 1,
                                fieldBuffer.length,
                                fieldIndex,
                            )
                        }
                    }
                }

                // 이스케이프 상태: 다음 문자로 변환 결정
                state == State.ESCAPE -> {
                    when (ch.toChar()) {
                        't' -> fieldBuffer.append('\t')
                        'n' -> fieldBuffer.append('\n')
                        'r' -> fieldBuffer.append('\r')
                        '\\' -> fieldBuffer.append('\\')
                        else -> {
                            // 알 수 없는 이스케이프 — 리터럴로 보존
                            fieldBuffer.append('\\')
                            fieldBuffer.append(ch.toChar())
                        }
                    }
                    state = State.START_FIELD
                }
            }
        }
    }

    /**
     * 현재 필드 버퍼를 완성하고 최종 값을 반환한다.
     *
     * - [TsvSettings.trimValues]가 `true`이면 앞뒤 공백을 제거한다
     * - [TsvSettings.emptyValueAsNull]이 `true`이고 값이 빈 문자열이면 `null`을 반환한다
     */
    private fun finishField(): String? {
        var raw = fieldBuffer.toString()
        fieldBuffer.clear()
        if (settings.trimValues) raw = raw.trim()
        return if (raw.isEmpty() && settings.emptyValueAsNull) null else raw
    }

    /**
     * 내부 Reader를 닫는다. 오류는 무시한다.
     */
    override fun close() {
        runCatching { bufferedReader.close() }
    }
}
