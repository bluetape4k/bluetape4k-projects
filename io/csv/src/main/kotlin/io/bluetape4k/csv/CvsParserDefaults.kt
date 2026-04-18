package io.bluetape4k.csv

/**
 * CSV/TSV 컬럼 하나에 허용되는 최대 문자 수 기본값입니다.
 *
 * ## 동작/계약
 * - [CsvSettings] 및 [TsvSettings] 기본 설정에서 공통으로 사용됩니다.
 * - 큰 필드를 처리할 수 있도록 100,000 문자로 설정되어 있습니다.
 *
 * ```kotlin
 * val limit = MAX_CHARS_PER_COLUMN
 * // limit == 100000
 * ```
 */
const val MAX_CHARS_PER_COLUMN: Int = 100_000
