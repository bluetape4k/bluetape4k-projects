package io.bluetape4k.csv

import com.univocity.parsers.csv.CsvParserSettings
import com.univocity.parsers.csv.CsvWriterSettings
import com.univocity.parsers.tsv.TsvParserSettings
import com.univocity.parsers.tsv.TsvWriterSettings

const val MAX_CHARS_PER_COLUMN: Int = 100_000

/**
 * CSV 파서 설정의 기본값입니다.
 */
@JvmField
val DefaultCsvParserSettings: CsvParserSettings = CsvParserSettings().apply {
    trimValues(true)
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * TSV 파서 설정의 기본값입니다.
 */
@JvmField
val DefaultTsvParserSettings: TsvParserSettings = TsvParserSettings().apply {
    trimValues(true)
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * CSV Writer 설정의 기본값입니다.
 */
@JvmField
val DefaultCsvWriterSettings: CsvWriterSettings = CsvWriterSettings().apply {
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}

/**
 * TSV Writer 설정의 기본값입니다.
 */
@JvmField
val DefaultTsvWriterSettings: TsvWriterSettings = TsvWriterSettings().apply {
    maxCharsPerColumn = MAX_CHARS_PER_COLUMN
}
