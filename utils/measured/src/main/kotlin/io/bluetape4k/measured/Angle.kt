package io.bluetape4k.measured

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 각도 단위를 나타냅니다.
 */
open class Angle(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        /** 라디안은 기준 단위입니다. */
        @JvmField val radians: Angle = Angle("rad")

        /** 도는 라디안 기준 비율을 사용합니다. */
        @JvmField val degrees: Angle = object: Angle("°", PI / 180.0) {
            override val spaceBetweenMagnitude: Boolean = false
        }

        /** 사인 함수를 수행합니다. */
        @JvmStatic
        fun sin(angle: Measure<Angle>): Double = sin(angle `in` radians)

        /** 코사인 함수를 수행합니다. */
        @JvmStatic
        fun cos(angle: Measure<Angle>): Double = cos(angle `in` radians)

        /** 탄젠트 함수를 수행합니다. */
        @JvmStatic
        fun tan(angle: Measure<Angle>): Double = tan(angle `in` radians)

        /** 아크사인을 라디안 각도로 반환합니다. */
        @JvmStatic
        fun asin(value: Double): Measure<Angle> = kotlin.math.asin(value) * radians

        /** 아크코사인을 라디안 각도로 반환합니다. */
        @JvmStatic
        fun acos(value: Double): Measure<Angle> = kotlin.math.acos(value) * radians

        /** 아크탄젠트를 라디안 각도로 반환합니다. */
        @JvmStatic
        fun atan(value: Double): Measure<Angle> = kotlin.math.atan(value) * radians

        /** atan2 결과를 라디안 각도로 반환합니다. */
        @JvmStatic
        fun atan2(y: Double, x: Double): Measure<Angle> = kotlin.math.atan2(y, x) * radians
    }
}

/**
 * 숫자를 도 단위 측정값으로 변환합니다.
 */
fun Number.degrees(): Measure<Angle> = this * Angle.degrees

/**
 * 숫자를 라디안 단위 측정값으로 변환합니다.
 */
fun Number.radians(): Measure<Angle> = this * Angle.radians

/**
 * 각도를 [0°, 360°) 범위로 정규화합니다.
 */
fun Measure<Angle>.normalize(): Measure<Angle> {
    var degree = (this `in` Angle.degrees) % 360.0
    if (degree < 0) degree += 360.0
    return degree * Angle.degrees
}
