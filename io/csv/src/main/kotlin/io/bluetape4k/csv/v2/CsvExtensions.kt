package io.bluetape4k.csv.v2

import io.bluetape4k.csv.Record
import io.bluetape4k.csv.internal.ArrayRecord
import io.bluetape4k.csv.internal.HeaderIndex

/**
 * [Record] V1 인터페이스를 [CsvRow] V2 타입으로 변환한다.
 *
 * ```kotlin
 * val csvRow: CsvRow = record.toCsvRow()
 * ```
 */
fun Record.toCsvRow(): CsvRow = CsvRow(
    values = values.toList(),
    headers = headers?.toList(),
    rowNumber = rowNumber,
)

/**
 * [CsvRow] V2 타입을 [Record] V1 인터페이스로 변환한다.
 *
 * 동일 모듈 내 테스트 전용 — 공개 API가 아닙니다.
 */
internal fun CsvRow.toRecord(): Record {
    val headerIndex: HeaderIndex? = headers?.let { hs ->
        HeaderIndex.of(hs.toTypedArray())
    }
    return ArrayRecord(
        rawValues = values.toTypedArray(),
        _headers = headers?.toTypedArray(),
        headerIndex = headerIndex,
        rowNumber = rowNumber,
    )
}

/**
 * CSV 전용 [FlowCsvReader] DSL 빌더.
 *
 * ```kotlin
 * val reader = csvReader {
 *     trimValues = true
 *     emptyValueAsNull = false
 * }
 * ```
 *
 * @param block [CsvReaderConfig] 설정 블록
 */
fun csvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader =
    FlowCsvReaderImpl(CsvReaderConfig().apply(block))

/**
 * TSV 전용 [FlowCsvReader] DSL 빌더.
 * `delimiter`는 항상 `'\t'`로 강제됩니다.
 *
 * ```kotlin
 * val reader = tsvReader { trimValues = true }
 * ```
 *
 * @param block [CsvReaderConfig] 설정 블록 (delimiter 설정은 무시됨)
 */
fun tsvReader(block: CsvReaderConfig.() -> Unit = {}): FlowCsvReader =
    FlowCsvReaderImpl(CsvReaderConfig().apply(block).also { it.delimiter = '\t'; it.lineSeparator = "\n" })
