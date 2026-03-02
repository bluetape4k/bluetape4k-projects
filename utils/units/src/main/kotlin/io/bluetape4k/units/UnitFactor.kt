package io.bluetape4k.units

/**
 * 길이 단위 환산 상수를 제공합니다.
 *
 * ## 동작/계약
 * - 각 상수는 "1 meter 당 해당 단위 수"를 나타냅니다.
 * - 변환 계산용 고정 상수이며 상태를 가지지 않습니다.
 *
 * ```kotlin
 * val inches = UnitFactor.INCH_IN_METER
 * // inches == 39.37
 * ```
 */
object UnitFactor {

    const val INCH_IN_METER = 39.37
    const val FEET_IN_METER = 3.2809
    const val YARD_IN_METER = 1.0936
    const val MILE_IN_METER = 1609.344

}
