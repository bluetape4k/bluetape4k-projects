package io.bluetape4k.csv.internal

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.logging.KLogging
import java.io.Closeable
import java.io.Writer

/**
 * [CsvSettings]를 기반으로 CSV 라인을 출력하는 Writer.
 *
 * 내부적으로 [DelimitedWriter]를 사용하며, [CsvSettings]에 정의된 구분자, 인용 문자,
 * 이스케이프 문자, 레코드 구분자를 그대로 위임합니다.
 *
 * ## null/빈 문자열 처리 정책
 * - `null` → 인용 없는 빈 필드 (roundtrip: 읽을 때 null로 복원)
 * - `""` (빈 문자열) → `""` 인용 출력 (roundtrip: 읽을 때 빈 문자열로 복원)
 *
 * @param writer 출력 대상 Writer
 * @param settings CSV 설정. 기본값은 [CsvSettings.DEFAULT]
 */
internal class CsvLineWriter(
    writer: Writer,
    settings: CsvSettings = CsvSettings.DEFAULT,
) : Closeable {

    companion object : KLogging()

    private val delegate = DelimitedWriter(
        writer = writer,
        delimiter = settings.delimiter,
        quote = settings.quote,
        quoteEscape = settings.quoteEscape,
        lineSeparator = settings.lineSeparator,
    )

    /**
     * 한 행을 CSV 형식으로 출력합니다.
     *
     * @param fields 출력할 필드 목록
     */
    fun writeRow(fields: Iterable<*>) = delegate.writeRow(fields)

    /**
     * Writer를 닫습니다.
     */
    override fun close() = delegate.close()
}
