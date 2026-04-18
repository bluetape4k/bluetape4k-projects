package io.bluetape4k.csv.v2

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.math.BigDecimal

/**
 * CSV/TSV V2 API의 불변 레코드 타입.
 *
 * [io.bluetape4k.csv.Record] V1 인터페이스와 달리 data class로 제공되어
 * copy, equals, hashCode, toString이 자동으로 제공됩니다.
 *
 * ## 생성 방법
 * ```kotlin
 * // V1 Record에서 변환
 * val csvRow = record.toCsvRow()
 *
 * // FlowCsvReader가 자동으로 생성
 * csvReader { trimValues = true }.read(inputStream).collect { row -> ... }
 * ```
 *
 * ## null vs 빈 문자열
 * - `null` — 인용 없는 빈 필드 (emptyValueAsNull=true 시)
 * - `""` — 인용된 빈 필드 `""` (빈 문자열 보존)
 *
 * @param values 원시 문자열 값 목록 (null 가능 원소 포함)
 * @param headers 헤더명 목록 (skipHeaders=true 시에만 non-null)
 * @param rowNumber 1-based 행 번호
 */
data class CsvRow(
    val values: List<String?>,
    val headers: List<String>?,
    val rowNumber: Long,
) : Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    /** 컬럼 수. */
    val size: Int get() = values.size

    /**
     * 인덱스로 원시 문자열 값을 반환한다.
     * 범위 초과이면 null 반환.
     */
    fun getString(index: Int): String? = values.getOrNull(index)

    /**
     * 헤더명으로 원시 문자열 값을 반환한다.
     * 헤더가 없거나 이름을 찾을 수 없으면 null 반환.
     */
    fun getString(name: String): String? {
        val idx = headers?.indexOf(name)?.takeIf { it >= 0 } ?: return null
        return getString(idx)
    }

    fun getIntOrNull(index: Int): Int? = getString(index)?.trim()?.toIntOrNull()
    fun getIntOrNull(name: String): Int? = getString(name)?.trim()?.toIntOrNull()

    fun getLongOrNull(index: Int): Long? = getString(index)?.trim()?.toLongOrNull()
    fun getLongOrNull(name: String): Long? = getString(name)?.trim()?.toLongOrNull()

    fun getDoubleOrNull(index: Int): Double? = getString(index)?.trim()?.toDoubleOrNull()
    fun getDoubleOrNull(name: String): Double? = getString(name)?.trim()?.toDoubleOrNull()

    fun getFloatOrNull(index: Int): Float? = getString(index)?.trim()?.toFloatOrNull()
    fun getFloatOrNull(name: String): Float? = getString(name)?.trim()?.toFloatOrNull()

    fun getBigDecimalOrNull(index: Int): BigDecimal? = getString(index)?.trim()?.toBigDecimalOrNull()
    fun getBigDecimalOrNull(name: String): BigDecimal? = getString(name)?.trim()?.toBigDecimalOrNull()

    fun getInt(index: Int, default: Int = 0): Int = getIntOrNull(index) ?: default
    fun getInt(name: String, default: Int = 0): Int = getIntOrNull(name) ?: default

    fun getLong(index: Int, default: Long = 0L): Long = getLongOrNull(index) ?: default
    fun getLong(name: String, default: Long = 0L): Long = getLongOrNull(name) ?: default

    fun getDouble(index: Int, default: Double = 0.0): Double = getDoubleOrNull(index) ?: default
    fun getDouble(name: String, default: Double = 0.0): Double = getDoubleOrNull(name) ?: default

    fun getBoolean(index: Int, default: Boolean = false): Boolean =
        getString(index)?.trim()?.toBoolean() ?: default

    fun getBoolean(name: String, default: Boolean = false): Boolean =
        getString(name)?.trim()?.toBoolean() ?: default
}
