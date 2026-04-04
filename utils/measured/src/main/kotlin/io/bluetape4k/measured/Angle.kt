package io.bluetape4k.measured

import io.bluetape4k.measured.Angle.Companion.degrees
import io.bluetape4k.measured.Angle.Companion.radians
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 각도 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - 기준 단위는 라디안([radians])이며 도([degrees])는 `PI/180` 비율을 사용합니다.
 * - 삼각함수는 입력 각도를 라디안으로 변환해 계산합니다.
 *
 * ```kotlin
 * val rad = 180.degrees() `in` Angle.radians
 * // rad == PI
 * ```
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

        /**
         * 사인 함수를 수행합니다.
         *
         * ```kotlin
         * val result = Angle.sin(90.degrees())
         * // result == 1.0
         * ```
         */
        @JvmStatic
        fun sin(angle: Measure<Angle>): Double = sin(angle `in` radians)

        /**
         * 코사인 함수를 수행합니다.
         *
         * ```kotlin
         * val result = Angle.cos(0.degrees())
         * // result == 1.0
         * ```
         */
        @JvmStatic
        fun cos(angle: Measure<Angle>): Double = cos(angle `in` radians)

        /**
         * 탄젠트 함수를 수행합니다.
         *
         * ```kotlin
         * val result = Angle.tan(45.degrees())
         * // result == 1.0
         * ```
         */
        @JvmStatic
        fun tan(angle: Measure<Angle>): Double = tan(angle `in` radians)

        /**
         * 아크사인을 라디안 각도로 반환합니다.
         *
         * ```kotlin
         * val angle = Angle.asin(1.0)
         * // angle `in` Angle.degrees == 90.0
         * ```
         */
        @JvmStatic
        fun asin(value: Double): Measure<Angle> = kotlin.math.asin(value) * radians

        /**
         * 아크코사인을 라디안 각도로 반환합니다.
         *
         * ```kotlin
         * val angle = Angle.acos(1.0)
         * // angle `in` Angle.degrees == 0.0
         * ```
         */
        @JvmStatic
        fun acos(value: Double): Measure<Angle> = kotlin.math.acos(value) * radians

        /**
         * 아크탄젠트를 라디안 각도로 반환합니다.
         *
         * ```kotlin
         * val angle = Angle.atan(1.0)
         * // angle `in` Angle.degrees == 45.0
         * ```
         */
        @JvmStatic
        fun atan(value: Double): Measure<Angle> = kotlin.math.atan(value) * radians

        /**
         * atan2 결과를 라디안 각도로 반환합니다.
         *
         * ```kotlin
         * val angle = Angle.atan2(1.0, 1.0)
         * // angle `in` Angle.degrees == 45.0
         * ```
         */
        @JvmStatic
        fun atan2(y: Double, x: Double): Measure<Angle> = kotlin.math.atan2(y, x) * radians
    }
}

/**
 * 숫자를 도 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 새 [Measure]를 생성하며 수신 값을 변경하지 않습니다.
 *
 * ```kotlin
 * val angle = 90.degrees()
 * // angle `in` Angle.radians == PI / 2
 * ```
 */
fun Number.degrees(): Measure<Angle> = this * Angle.degrees

/**
 * 숫자를 라디안 단위 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - 수치값을 라디안 기준 [Measure]로 감쌉니다.
 *
 * ```kotlin
 * val angle = PI.radians()
 * // angle `in` Angle.degrees == 180.0
 * ```
 */
fun Number.radians(): Measure<Angle> = this * Angle.radians

/**
 * 각도를 [0°, 360°) 범위로 정규화합니다.
 *
 * ## 동작/계약
 * - 음수/360도 초과 값도 모듈로 연산으로 정규화합니다.
 * - 결과 단위는 도([Angle.degrees])입니다.
 *
 * ```kotlin
 * val normalized = (-30).degrees().normalize()
 * // normalized `in` Angle.degrees == 330.0
 * ```
 */
fun Measure<Angle>.normalize(): Measure<Angle> {
    var degree = (this `in` Angle.degrees) % 360.0
    if (degree < 0) degree += 360.0
    return degree * Angle.degrees
}
