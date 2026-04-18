package io.bluetape4k.csv

import io.bluetape4k.csv.internal.ArrayRecord
import io.bluetape4k.csv.internal.HeaderIndex

/**
 * CSV/TSV 레코드 생성 팩토리.
 *
 * PR 1에서는 [ArrayRecord]를 반환하며, PR 2에서 `Record` 인터페이스 반환으로 전환됩니다.
 */
object RecordFactory {

    /**
     * 원시 값 배열, 헤더, 행 번호로 [ArrayRecord]를 생성합니다.
     *
     * @param values 원시 값 배열 (null 가능)
     * @param headers 헤더명 배열 (skipHeaders=true인 경우만 non-null)
     * @param rowNumber 1-based 행 번호
     * @return 생성된 [ArrayRecord]
     */
    internal fun recordOf(
        values: Array<String?>,
        headers: Array<String>? = null,
        rowNumber: Long,
    ): ArrayRecord {
        val headerIndex = headers?.let { HeaderIndex.of(it) }
        return ArrayRecord(
            rawValues = values,
            _headers = headers,
            headerIndex = headerIndex,
            rowNumber = rowNumber,
        )
    }
}
