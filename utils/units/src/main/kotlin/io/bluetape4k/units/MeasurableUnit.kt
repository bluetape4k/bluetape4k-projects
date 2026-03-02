package io.bluetape4k.units

/**
 * 단위 이름과 환산 계수를 제공하는 계약입니다.
 *
 * ## 동작/계약
 * - [unitName]은 문자열 파싱/표시용 식별자입니다.
 * - [factor]는 각 물리량의 기준 단위 대비 배율입니다.
 * - 구현체는 보통 `enum class`로 제공되며 불변 값으로 사용됩니다.
 *
 * ```kotlin
 * val unit = LengthUnit.METER
 * // unit.unitName == "m"
 * // unit.factor == 1000.0
 * ```
 */
interface MeasurableUnit {

    /**
     * 단위를 나타내는 문자열 (예: "m", "cm", "mm", "pa", "atm")
     */
    val unitName: String

    /**
     * 단위 변환을 위한 factor 값
     */
    val factor: Double
}
