package io.bluetape4k.units

/**
 * SI 배수/분수 접두사 계수를 제공합니다.
 *
 * ## 동작/계약
 * - 상수는 단위 변환 factor 계산에 사용하는 불변 값입니다.
 * - 10의 거듭제곱 배율을 그대로 노출하며 런타임 할당이 없습니다.
 *
 * ```kotlin
 * val kilo = UnitPrefix.KILO
 * val milli = UnitPrefix.MILLI
 * // kilo == 1.0e3
 * // milli == 1.0e-3
 * ```
 */
object UnitPrefix {

    // Multiples
    const val DECA = 10.0
    const val HECTO = 1.0e2
    const val KILO = 1.0e3
    const val MEGA = 1.0e6
    const val GIGA = 1.0e9
    const val TERA = 1.0e12
    const val PETA = 1.0e15
    const val EXA = 1.0e18
    const val ZETTA = 1.0e21
    const val YOTTA = 1.0e24

    // Fractions
    const val DECI = 1.0e-1
    const val CENTI = 1.0e-2
    const val MILLI = 1.0e-3
    const val MICRO = 1.0e-6
    const val PICO = 1.0e-9
    const val FEMTO = 1.0e-15
    const val ATTO = 1.0e-18
    const val ZEPTO = 1.0e-21
    const val YOCTO = 1.0e-24

}
