package io.bluetape4k.csv.internal

import io.bluetape4k.csv.Record
import io.bluetape4k.logging.KLogging
import java.math.BigDecimal

/**
 * CSV/TSV 파싱 결과를 저장하는 내부 [Record] 구현체.
 *
 * @param rawValues 원시 값 배열 (null 가능). 방어적 복사로 저장.
 * @param _headers 헤더명 배열 (skipHeaders=true인 경우만 존재). 방어적 복사로 저장.
 * @param headerIndex 헤더명 → 인덱스 매핑 (헤더가 있을 때만 non-null)
 * @param rowNumber 1-based 행 번호
 */
internal class ArrayRecord(
    rawValues: Array<String?>,
    _headers: Array<String>?,
    val headerIndex: HeaderIndex?,
    override val rowNumber: Long,
) : Record {

    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    /** 방어적 복사된 원시 값 배열. */
    val rawValues: Array<String?> = rawValues.copyOf()

    /** 방어적 복사된 헤더명 배열. */
    override val headers: Array<String>? = _headers?.copyOf()

    override val size: Int get() = rawValues.size

    override val values: Array<String?> get() = rawValues.copyOf()

    // ── index 기반 getter ────────────────────────────────

    override fun getString(index: Int): String? = rawValues.getOrNull(index)

    override fun getString(name: String): String? {
        val idx = headerIndex?.indexOf(name) ?: return null
        return getString(idx)
    }

    /**
     * 타입 변환 내부 헬퍼. T : Any 제약으로 null defaultValue를 컴파일 타임에 차단.
     */
    private fun <T : Any> convert(raw: String?, defaultValue: T, converter: (String) -> T?): T {
        if (raw == null) return defaultValue
        return runCatching { converter(raw) }.getOrNull() ?: defaultValue
    }

    override fun <T : Any> getValue(index: Int, defaultValue: T): T =
        convert(getString(index), defaultValue) { raw ->
            @Suppress("UNCHECKED_CAST")
            when (defaultValue) {
                is String -> raw as T
                is Int -> raw.trim().toInt() as T
                is Long -> raw.trim().toLong() as T
                is Double -> raw.trim().toDouble() as T
                is Float -> raw.trim().toFloat() as T
                is Boolean -> raw.trim().toBoolean() as T
                is BigDecimal -> raw.trim().toBigDecimal() as T
                else -> throw IllegalArgumentException("지원하지 않는 타입: ${defaultValue::class}")
            }
        }

    override fun <T : Any> getValue(name: String, defaultValue: T): T {
        val idx = headerIndex?.indexOf(name) ?: return defaultValue
        return getValue(idx, defaultValue)
    }

    // ── nullable getter ──────────────────────────────────

    override fun getIntOrNull(index: Int): Int? = getString(index)?.trim()?.toIntOrNull()
    override fun getIntOrNull(name: String): Int? = getString(name)?.trim()?.toIntOrNull()
    override fun getLongOrNull(index: Int): Long? = getString(index)?.trim()?.toLongOrNull()
    override fun getLongOrNull(name: String): Long? = getString(name)?.trim()?.toLongOrNull()
    override fun getDoubleOrNull(index: Int): Double? = getString(index)?.trim()?.toDoubleOrNull()
    override fun getDoubleOrNull(name: String): Double? = getString(name)?.trim()?.toDoubleOrNull()
    override fun getFloatOrNull(index: Int): Float? = getString(index)?.trim()?.toFloatOrNull()
    override fun getFloatOrNull(name: String): Float? = getString(name)?.trim()?.toFloatOrNull()
    override fun getBigDecimalOrNull(index: Int): BigDecimal? = getString(index)?.trim()?.toBigDecimalOrNull()
    override fun getBigDecimalOrNull(name: String): BigDecimal? = getString(name)?.trim()?.toBigDecimalOrNull()

    override fun toString(): String =
        "ArrayRecord(row=$rowNumber, size=$size, values=${rawValues.contentToString()})"
}
