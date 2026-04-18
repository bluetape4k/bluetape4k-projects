package io.bluetape4k.csv.internal

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.math.BigDecimal

/**
 * CSV/TSV 파싱 결과를 저장하는 내부 구현체.
 *
 * PR 1에서는 [Serializable]을 직접 구현하며, PR 2에서 public `Record` 인터페이스로 전환됩니다.
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
    val rowNumber: Long,
) : Serializable {

    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }

    /** 방어적 복사된 원시 값 배열. */
    val rawValues: Array<String?> = rawValues.copyOf()

    /** 방어적 복사된 헤더명 배열. */
    val headers: Array<String>? = _headers?.copyOf()

    /** 필드 수. */
    val size: Int get() = rawValues.size

    /** 원시 값 배열 (외부 노출용 복사본). */
    val values: Array<String?> get() = rawValues.copyOf()

    // ────────────────────────────────────────
    // index 기반 getter
    // ────────────────────────────────────────

    /**
     * 지정 인덱스의 원시 문자열 값을 반환한다.
     * 범위 초과이면 null 반환.
     */
    fun getString(index: Int): String? = rawValues.getOrNull(index)

    /**
     * 헤더명으로 원시 문자열 값을 반환한다.
     * 헤더가 없거나 이름을 찾을 수 없으면 null 반환.
     */
    fun getString(name: String): String? {
        val idx = headerIndex?.indexOf(name) ?: return null
        return getString(idx)
    }

    /**
     * 타입 변환 내부 헬퍼. defaultValue: T에서 T : Any 제약으로 null 컴파일 타임 차단.
     *
     * @param raw 원시 문자열 (null 가능)
     * @param defaultValue null이거나 변환 실패 시 반환할 기본값 (non-null 강제)
     * @param converter 변환 함수
     */
    private fun <T : Any> convert(raw: String?, defaultValue: T, converter: (String) -> T?): T {
        if (raw == null) return defaultValue
        return runCatching { converter(raw) }.getOrNull() ?: defaultValue
    }

    /**
     * 지정 인덱스 값을 `defaultValue`의 타입으로 변환하여 반환한다.
     * 변환 실패 시 `defaultValue` 반환.
     */
    fun <T : Any> getValue(index: Int, defaultValue: T): T =
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

    /**
     * 헤더명으로 값을 찾아 `defaultValue`의 타입으로 변환하여 반환한다.
     * 헤더가 없거나 변환 실패 시 `defaultValue` 반환.
     */
    fun <T : Any> getValue(name: String, defaultValue: T): T {
        val idx = headerIndex?.indexOf(name) ?: return defaultValue
        return getValue(idx, defaultValue)
    }

    // nullable getter들
    fun getIntOrNull(index: Int): Int? = getString(index)?.trim()?.toIntOrNull()
    fun getIntOrNull(name: String): Int? = getString(name)?.trim()?.toIntOrNull()
    fun getLongOrNull(index: Int): Long? = getString(index)?.trim()?.toLongOrNull()
    fun getLongOrNull(name: String): Long? = getString(name)?.trim()?.toLongOrNull()
    fun getDoubleOrNull(index: Int): Double? = getString(index)?.trim()?.toDoubleOrNull()
    fun getDoubleOrNull(name: String): Double? = getString(name)?.trim()?.toDoubleOrNull()
    fun getFloatOrNull(index: Int): Float? = getString(index)?.trim()?.toFloatOrNull()
    fun getFloatOrNull(name: String): Float? = getString(name)?.trim()?.toFloatOrNull()
    fun getBigDecimal(index: Int): BigDecimal? = getString(index)?.trim()?.toBigDecimalOrNull()
    fun getBigDecimal(name: String): BigDecimal? = getString(name)?.trim()?.toBigDecimalOrNull()

    override fun toString(): String =
        "ArrayRecord(row=$rowNumber, size=$size, values=${rawValues.contentToString()})"
}
