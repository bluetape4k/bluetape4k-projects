package io.bluetape4k.units

import java.io.Serializable

/**
 * 단위 기반 물리량의 공통 계약입니다.
 *
 * ## 동작/계약
 * - [value]는 각 타입의 기준 단위 값으로 저장됩니다.
 * - [valueBy]는 `value / unit.factor` 규칙으로 환산값을 계산합니다.
 * - [compareTo]는 기준 단위 값으로 비교합니다.
 *
 * ```kotlin
 * val length = 100.0.meter()
 * val km = length.valueBy(LengthUnit.KILOMETER)
 * // km == 0.1
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
     * ## 동작/계약
     * - 구현체는 새 인스턴스를 반환해야 하며 수신 객체를 변경하지 않습니다.
     *
     * ```kotlin
     * val km = 1000.0.meter().convertTo(LengthUnit.KILOMETER)
     * // km.valueBy(LengthUnit.KILOMETER) == 1.0
     * ```
     *
     * @param newUnit 변환할 대상 단위
     * @return 새로운 단위로 변환된 측정값
     */
    fun convertTo(newUnit: T): Measurable<T>

    /**
     * 지정된 단위로 표현된 값을 반환합니다.
     *
     * ## 동작/계약
     * - `factor == 0` 단위(예: 일부 온도 단위)는 구현체가 `valueBy`를 override해서 처리할 수 있습니다.
     *
     * @param unit 값을 표현할 단위
     * @return 지정된 단위로 변환된 값 (Double)
     */
    fun valueBy(unit: T): Double = value / unit.factor

    /**
     * 사람이 읽기 쉬운 형식으로 값을 반환합니다.
     *
     * ## 동작/계약
     * - 자동 단위 선택 규칙은 구현체에 따라 다를 수 있습니다.
     *
     * ```kotlin
     * val text = 1024.0.kbytes().toHuman()
     * // text.isNotBlank() == true
     * ```
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
