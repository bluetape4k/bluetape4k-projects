package io.bluetape4k.units

import java.io.Serializable

/**
 * 측정 가능한 물리량을 나타내는 기본 인터페이스
 *
 * 이 인터페이스는 모든 단위 기반 측정값(길이, 무게, 면적 등)의 공통 동작을 정의합니다.
 * [Comparable]을 구현하여 측정값 간 비교가 가능하며, [Serializable]을 구현하여 직렬화를 지원합니다.
 *
 * @param T 측정 단위 타입 ([MeasurableUnit]을 구현한 enum 등)
 *
 * ```
 * // Length, Weight, Area 등에서 사용 예시
 * val length = 100.0.meter()
 * val weight = 50.0.kilogram()
 *
 * // 단위 변환
 * val lengthInKm = length.convertTo(LengthUnit.KILOMETER)
 * val weightInGrams = weight.valueBy(WeightUnit.GRAM)
 *
 * // 사람이 읽기 쉬운 형식으로 변환
 * println(length.toHuman())  // "100.0 m"
 * println(weight.toHuman())  // "50.0 kg"
 * ```
 */
interface Measurable<T: MeasurableUnit>: Comparable<Measurable<T>>, Serializable {
    /**
     * 기본 단위로 표현된 측정값
     *
     * 예: Length는 mm, Weight는 g, Area는 mm^2 등을 기본 단위로 사용
     */
    val value: Double

    /**
     * 새로운 단위로 변환된 측정값을 반환합니다.
     *
     * @param newUnit 변환할 대상 단위
     * @return 새로운 단위로 변환된 측정값
     */
    fun convertTo(newUnit: T): Measurable<T>

    /**
     * 지정된 단위로 표현된 값을 반환합니다.
     *
     * @param unit 값을 표현할 단위
     * @return 지정된 단위로 변환된 값 (Double)
     */
    fun valueBy(unit: T): Double = value / unit.factor

    /**
     * 사람이 읽기 쉬운 형식으로 값을 반환합니다.
     * 자동으로 적절한 단위를 선택하여 표시합니다.
     *
     * @return 예: "100.0 m", "50.0 kg"
     */
    fun toHuman(): String

    /**
     * 지정된 단위로 사람이 읽기 쉬운 형식으로 값을 반환합니다.
     *
     * @param unit 표시할 단위
     * @return 예: "100.0 m" (unit이 METER인 경우)
     */
    fun toHuman(unit: T): String = formatUnit(valueBy(unit), unit.unitName)

    /**
     * 다른 측정값과 비교합니다.
     * 기본 단위([value])를 기준으로 비교합니다.
     *
     * @param other 비교할 대상 측정값
     * @return 비교 결과 (음수: 작음, 0: 같음, 양수: 큼)
     */
    override fun compareTo(other: Measurable<T>): Int = value.compareTo(other.value)
}
