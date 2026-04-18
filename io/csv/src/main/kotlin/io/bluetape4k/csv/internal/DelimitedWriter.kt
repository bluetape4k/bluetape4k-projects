package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import java.io.Closeable
import java.io.Writer

/**
 * RFC 4180 기반 구분자 방식 CSV/TSV 라이터.
 * null/빈 문자열 roundtrip 정책을 보장합니다.
 *
 * - `null` → 인용 없는 빈 필드 (구분자만 출력)
 * - `""` (빈 문자열) → `""` 인용 출력 (roundtrip 보장)
 *
 * @param writer 출력 대상 Writer
 * @param delimiter 필드 구분 문자
 * @param quote 인용 문자
 * @param quoteEscape 인용 문자 이스케이프 문자 (RFC 4180: doubled-quote)
 * @param lineSeparator 레코드 구분자
 */
internal class DelimitedWriter(
    private val writer: Writer,
    private val delimiter: Char,
    private val quote: Char,
    private val quoteEscape: Char,
    private val lineSeparator: String,
) : Closeable {

    companion object : KLogging()

    /**
     * 필드가 인용 문자로 감싸야 하는지 판단합니다.
     *
     * 다음 조건 중 하나라도 해당하면 인용이 필요합니다:
     * - 필드 앞 또는 뒤에 공백이 있는 경우
     * - 필드에 [delimiter] 문자가 포함된 경우
     * - 필드에 [quote] 문자가 포함된 경우
     * - 필드에 CR(`\r`) 또는 LF(`\n`)가 포함된 경우
     *
     * 빈 문자열은 이 메서드에서 `false`를 반환하지만, 호출자에서 별도로 인용 처리합니다.
     */
    internal fun needsQuoting(s: String): Boolean {
        if (s.isEmpty()) return false
        if (s[0] == ' ' || s[s.length - 1] == ' ') return true
        for (c in s) {
            if (c == delimiter || c == quote || c == '\r' || c == '\n') return true
        }
        return false
    }

    /**
     * 필드를 인용 문자로 감싸 출력합니다.
     *
     * 내부 [quote] 문자는 RFC 4180 doubled-quote 방식으로 이스케이프됩니다.
     *
     * @param s 출력할 문자열
     */
    internal fun writeQuoted(s: String) {
        writer.write(quote.code)
        for (c in s) {
            if (c == quote) {
                writer.write(quoteEscape.code)  // RFC 4180: doubled-quote
                writer.write(quote.code)
            } else {
                writer.write(c.code)
            }
        }
        writer.write(quote.code)
    }

    /**
     * 한 행을 구분자 형식으로 출력합니다.
     *
     * 필드별 출력 규칙:
     * - `null` 값 → 인용 없는 빈 필드 (아무것도 출력하지 않음)
     * - `""` (빈 문자열) → `""` 인용 출력 (roundtrip 보장)
     * - 인용이 필요한 문자열 → 인용 출력
     * - 일반 문자열 → 그대로 출력
     *
     * @param fields 출력할 필드 목록
     */
    fun writeRow(fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) writer.write(delimiter.code)
            first = false

            when (field) {
                null -> { /* 인용 없는 빈 필드 — 아무것도 쓰지 않음 */ }
                is String -> {
                    if (field.isEmpty()) {
                        writeQuoted(field)  // "" → "" 인용 출력
                    } else if (needsQuoting(field)) {
                        writeQuoted(field)
                    } else {
                        writer.write(field)
                    }
                }
                else -> {
                    val str = field.toString()
                    if (needsQuoting(str)) writeQuoted(str) else writer.write(str)
                }
            }
        }
        writer.write(lineSeparator)
    }

    /**
     * Writer를 flush 후 close합니다.
     *
     * close 중 발생하는 예외는 로그 없이 무시됩니다.
     */
    override fun close() {
        runCatching {
            writer.flush()
            writer.close()
        }
    }
}
