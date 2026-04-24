package io.bluetape4k.csv.v2

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.MAX_CHARS_PER_COLUMN

/**
 * CSV/TSV V2 라이터 설정 (var 기반 mutable builder).
 *
 * DSL 빌더 함수 [csvWriter] / [tsvWriter]에서 사용됩니다.
 *
 * ## 사용 예
 * ```kotlin
 * val writer = csvWriter(outputWriter) {
 *     delimiter = ';'
 *     quoteAll = true
 * }
 * ```
 */
class CsvWriterConfig {
    /** 필드 구분 문자. 기본값: `,`. */
    var delimiter: Char = ','

    /** 인용 문자. 기본값: `"`. */
    var quote: Char = '"'

    /** 레코드 구분자. 기본값: `"\r\n"` (RFC 4180). */
    var lineSeparator: String = "\r\n"

    /**
     * 모든 필드를 인용(`"..."`) 출력할지 여부.
     * `true`이면 필요 여부와 관계없이 모든 필드를 인용 출력합니다.
     * `false`이면 RFC 4180 규칙에 따라 필요한 필드만 인용합니다.
     * 기본값: `false`.
     */
    var quoteAll: Boolean = false

    /** 컬럼당 최대 문자 수. 기본값: [MAX_CHARS_PER_COLUMN]. */
    var maxCharsPerColumn: Int = MAX_CHARS_PER_COLUMN

    /** 레코드당 최대 컬럼 수. 기본값: 512. */
    var maxColumns: Int = 512

    internal fun toCsvSettings(): CsvSettings = CsvSettings(
        delimiter = delimiter,
        quote = quote,
        quoteEscape = quote,
        lineSeparator = lineSeparator,
        maxCharsPerColumn = maxCharsPerColumn,
        maxColumns = maxColumns,
    )
}
