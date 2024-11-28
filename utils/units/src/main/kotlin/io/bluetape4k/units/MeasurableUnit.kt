package io.bluetape4k.units

/**
 * 측정 단위를 나타내는 인터페이스
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
