package io.bluetape4k.csv.v2

import io.bluetape4k.csv.CsvSettings
import io.bluetape4k.csv.MAX_CHARS_PER_COLUMN

/**
 * CSV/TSV V2 리더 설정 (var 기반 mutable builder).
 *
 * DSL 빌더 함수 [csvReader] / [tsvReader]에서 사용됩니다.
 *
 * ## 사용 예
 * ```kotlin
 * val reader = csvReader {
 *     delimiter = ';'
 *     trimValues = true
 *     emptyValueAsNull = false
 * }
 * ```
 */
class CsvReaderConfig {
    /** 필드 구분 문자. 기본값: `,`. */
    var delimiter: Char = ','

    /** 인용 문자. 기본값: `"`. */
    var quote: Char = '"'

    /** 레코드 구분자. 기본값: `"\r\n"` (RFC 4180). */
    var lineSeparator: String = "\r\n"

    /** 앞뒤 공백 제거 여부. reader 전용. 기본값: `false`. */
    var trimValues: Boolean = false

    /** 빈 줄 건너뛰기. 기본값: `true`. */
    var skipEmptyLines: Boolean = true

    /** 인용 없는 빈 필드 → `null` 변환 여부. 기본값: `true`. */
    var emptyValueAsNull: Boolean = true

    /** 인용 빈 필드(`""`) → `null` 변환 여부. 기본값: `false`. */
    var emptyQuotedAsNull: Boolean = false

    /** BOM 자동 감지/제거 여부. 기본값: `true`. */
    var detectBom: Boolean = true

    /** 컬럼당 최대 문자 수. 기본값: [MAX_CHARS_PER_COLUMN]. */
    var maxCharsPerColumn: Int = MAX_CHARS_PER_COLUMN

    /** 레코드당 최대 컬럼 수. 기본값: 512. */
    var maxColumns: Int = 512

    /** 내부 읽기 버퍼 크기(바이트). 기본값: 8192. */
    var bufferSize: Int = 8192

    internal fun toCsvSettings(): CsvSettings = CsvSettings(
        delimiter = delimiter,
        quote = quote,
        quoteEscape = quote,
        lineSeparator = lineSeparator,
        trimValues = trimValues,
        skipEmptyLines = skipEmptyLines,
        emptyValueAsNull = emptyValueAsNull,
        emptyQuotedAsNull = emptyQuotedAsNull,
        detectBom = detectBom,
        maxCharsPerColumn = maxCharsPerColumn,
        maxColumns = maxColumns,
        bufferSize = bufferSize,
    )
}
