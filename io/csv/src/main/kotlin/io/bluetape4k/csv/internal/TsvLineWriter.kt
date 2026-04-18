package io.bluetape4k.csv.internal

import io.bluetape4k.csv.TsvSettings
import io.bluetape4k.logging.KLogging
import java.io.Closeable
import java.io.Writer

/**
 * [TsvSettings]를 기반으로 TSV 라인을 출력하는 Writer.
 *
 * 탭(`\t`) 구분, 백슬래시 이스케이프 방식을 사용합니다. CSV와 달리 인용(quote) 문자를 사용하지 않습니다.
 *
 * ## 이스케이프 규칙
 * - `\t` (탭) → `\\t`
 * - `\n` (LF) → `\\n`
 * - `\r` (CR) → `\\r`
 * - `\\` (백슬래시) → `\\\\`
 *
 * ## null/빈 문자열 처리 정책
 * - `null` → 빈 필드 (아무것도 출력하지 않음)
 * - `""` (빈 문자열) → 빈 필드 (TSV에서 null과 구분 불가)
 *
 * @param writer 출력 대상 Writer
 * @param settings TSV 설정. 기본값은 [TsvSettings.DEFAULT]
 */
internal class TsvLineWriter(
    private val writer: Writer,
    private val settings: TsvSettings = TsvSettings.DEFAULT,
) : Closeable {

    companion object : KLogging()

    /**
     * 한 행을 TSV 형식으로 출력합니다.
     *
     * `null`은 빈 필드로 출력합니다. 각 필드 값은 [escape] 처리 후 출력됩니다.
     *
     * @param fields 출력할 필드 목록
     */
    fun writeRow(fields: Iterable<*>) {
        var first = true
        for (field in fields) {
            if (!first) writer.write('\t'.code)
            first = false
            when (field) {
                null -> { /* 빈 필드 */ }
                else -> writer.write(escape(field.toString()))
            }
        }
        writer.write(settings.lineSeparator)
    }

    /**
     * TSV 이스케이프 처리를 수행합니다.
     *
     * 이스케이프가 필요한 문자가 없으면 원본 문자열을 그대로 반환합니다.
     *
     * @param s 이스케이프할 문자열
     * @return 이스케이프 처리된 문자열
     */
    private fun escape(s: String): String {
        if (s.none { it == '\t' || it == '\n' || it == '\r' || it == '\\' }) return s
        return buildString(s.length + 4) {
            for (c in s) {
                when (c) {
                    '\\' -> append("\\\\")
                    '\t' -> append("\\t")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    else -> append(c)
                }
            }
        }
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
