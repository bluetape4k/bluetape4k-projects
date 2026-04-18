package io.bluetape4k.csv

import java.io.Serializable
import java.math.BigDecimal

/**
 * CSV/TSV 파싱 결과를 담는 공개 레코드 인터페이스.
 *
 * 행 단위 파싱 결과를 타입 안전하게 접근할 수 있는 계약을 정의합니다.
 * 인덱스 기반 접근과 헤더명 기반 접근을 모두 지원합니다.
 *
 * ```kotlin
 * val reader = CsvRecordReader()
 * reader.read(input, skipHeaders = true) { record ->
 *     val name = record.getString("name") ?: "unknown"
 *     val age  = record.getValue("age", 0)
 *     name to age
 * }.toList()
 * ```
 */
interface Record : Serializable {

    /** 1-based 행 번호 (skipHeaders=true 시 헤더 행 제외 기준). */
    val rowNumber: Long

    /** 필드 수. */
    val size: Int

    /** 원시 값 배열 복사본 (null 가능 원소 포함). */
    val values: Array<String?>

    /** 헤더명 배열 (skipHeaders=true일 때만 non-null). */
    val headers: Array<String>?

    // ── index 기반 접근 ──────────────────────────────────

    /**
     * 지정 인덱스의 원시 문자열 값을 반환한다.
     * 범위 초과이면 null 반환.
     */
    fun getString(index: Int): String?

    /**
     * 헤더명으로 원시 문자열 값을 반환한다.
     * 헤더가 없거나 이름을 찾을 수 없으면 null 반환.
     */
    fun getString(name: String): String?

    /**
     * 지정 인덱스 값을 [defaultValue]의 타입으로 변환하여 반환한다.
     * 변환 실패 시 [defaultValue]를 반환한다.
     *
     * 지원 타입: [String], [Int], [Long], [Double], [Float], [Boolean], [BigDecimal].
     */
    fun <T : Any> getValue(index: Int, defaultValue: T): T

    /**
     * 헤더명으로 값을 찾아 [defaultValue]의 타입으로 변환하여 반환한다.
     * 헤더가 없거나 변환 실패 시 [defaultValue]를 반환한다.
     */
    fun <T : Any> getValue(name: String, defaultValue: T): T

    // ── nullable 타입 변환 ───────────────────────────────

    fun getIntOrNull(index: Int): Int?
    fun getIntOrNull(name: String): Int?

    fun getLongOrNull(index: Int): Long?
    fun getLongOrNull(name: String): Long?

    fun getDoubleOrNull(index: Int): Double?
    fun getDoubleOrNull(name: String): Double?

    fun getFloatOrNull(index: Int): Float?
    fun getFloatOrNull(name: String): Float?

    fun getBigDecimalOrNull(index: Int): BigDecimal?
    fun getBigDecimalOrNull(name: String): BigDecimal?
}
