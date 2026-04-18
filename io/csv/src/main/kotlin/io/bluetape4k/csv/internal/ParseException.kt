package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * CSV/TSV 파싱 중 발생하는 예외.
 *
 * @param message 오류 메시지
 * @param rowNumber 오류가 발생한 행 번호 (1-based, human-facing)
 * @param columnNumber 오류가 발생한 열 번호 (1-based, human-facing)
 * @param fieldIndex 오류가 발생한 필드 인덱스 (0-based, 첫 번째 필드 = 0, 알 수 없으면 -1)
 */
class ParseException(
    message: String,
    val rowNumber: Long,
    val columnNumber: Int,
    val fieldIndex: Int = -1,
) : RuntimeException("$message (row=$rowNumber, col=$columnNumber, field=$fieldIndex)"), Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
