package io.bluetape4k.csv

import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriterSettings
import com.univocity.parsers.tsv.TsvParserSettings
import com.univocity.parsers.tsv.TsvWriterSettings

/**
 * CSV/TSV 컬럼 하나에 허용되는 최대 문자 수 기본값입니다.
 *
 * ## 동작/계약
 * - 파서/라이터 기본 설정에서 공통으로 사용됩니다.
 * - 큰 필드를 처리할 수 있도록 100,000 문자로 설정되어 있습니다.
 *
 * ```kotlin
 * val limit = MAX_CHARS_PER_COLUMN
 * // limit == 100000
 * ```
 */
const val MAX_CHARS_PER_COLUMN: Int = 100_000

/**
 * CSV 파서 기본 설정입니다.
 *
 * ## 동작/계약
 * - `trimValues(true)`가 적용되어 앞뒤 공백이 제거됩니다.
 * - `maxCharsPerColumn`은 [MAX_CHARS_PER_COLUMN]으로 제한됩니다.
 *
 * ```kotlin
 * val settings = DefaultCsvParserSettings
 * // settings.maxCharsPerColumn == 100000
 * ```
 */
@JvmField
val DefaultCsvParserSettings: CsvParserSettings = CsvParserSettings().apply {
    trimValues(true)
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * TSV 파서 기본 설정입니다.
 *
 * ## 동작/계약
 * - `trimValues(true)`가 적용됩니다.
 * - 컬럼 길이 제한은 [MAX_CHARS_PER_COLUMN]을 사용합니다.
 *
 * ```kotlin
 * val settings = DefaultTsvParserSettings
 * // settings.maxCharsPerColumn == 100000
 * ```
 */
@JvmField
val DefaultTsvParserSettings: TsvParserSettings = TsvParserSettings().apply {
    trimValues(true)
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * CSV Writer 기본 설정입니다.
 *
 * ## 동작/계약
 * - 컬럼당 최대 문자 수를 [MAX_CHARS_PER_COLUMN]으로 제한합니다.
 * - 재사용 가능한 singleton 설정 인스턴스입니다.
 *
 * ```kotlin
 * val settings = DefaultCsvWriterSettings
 * // settings.maxCharsPerColumn == 100000
 * ```
 */
@JvmField
val DefaultCsvWriterSettings: CsvWriterSettings = CsvWriterSettings().apply {
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * TSV Writer 기본 설정입니다.
 *
 * ## 동작/계약
 * - 컬럼당 최대 문자 수를 [MAX_CHARS_PER_COLUMN]으로 제한합니다.
 * - 재사용 가능한 singleton 설정 인스턴스입니다.
 *
 * ```kotlin
 * val settings = DefaultTsvWriterSettings
 * // settings.maxCharsPerColumn == 100000
 * ```
 */
@JvmField
val DefaultTsvWriterSettings: TsvWriterSettings = TsvWriterSettings().apply {
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}
