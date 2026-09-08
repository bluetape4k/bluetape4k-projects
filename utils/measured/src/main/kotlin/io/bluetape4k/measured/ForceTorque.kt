package io.bluetape4k.measured

/**
 * 힘 단위를 나타냅니다.
 *
 * 기준 단위는 뉴턴([newtons])이며 `kg*m/s²`로 정규화합니다.
 */
open class Force(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val newtons: Force = Force("N")

        @JvmField
        val milliNewtons: Force = Force("mN", 1.0e-3)

        @JvmField
        val kiloNewtons: Force = Force("kN", 1.0e3)

        @JvmField
        val megaNewtons: Force = Force("MN", 1.0e6)
    }
}

/**
 * 토크 단위를 나타냅니다.
 *
 * 힘과 길이의 곱과 차원은 같지만 에너지와 다른 물리량이므로 별도 의미 타입으로
 * 유지합니다. 기본 단위는 뉴턴미터([newtonMeters])입니다.
 */
open class Torque(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val newtonMeters: Torque = Torque("N·m")

        @JvmField
        val kiloNewtonMeters: Torque = Torque("kN·m", 1.0e3)
    }
}

/** 숫자를 뉴턴 단위 측정값으로 변환합니다. */
fun Number.newtons(): Measure<Force> = this * Force.newtons

/** 숫자를 밀리뉴턴 단위 측정값으로 변환합니다. */
fun Number.milliNewtons(): Measure<Force> = this * Force.milliNewtons

/** 숫자를 킬로뉴턴 단위 측정값으로 변환합니다. */
fun Number.kiloNewtons(): Measure<Force> = this * Force.kiloNewtons

/** 숫자를 메가뉴턴 단위 측정값으로 변환합니다. */
fun Number.megaNewtons(): Measure<Force> = this * Force.megaNewtons

/** 숫자를 뉴턴미터 단위 측정값으로 변환합니다. */
fun Number.newtonMeters(): Measure<Torque> = this * Torque.newtonMeters

/** 숫자를 킬로뉴턴미터 단위 측정값으로 변환합니다. */
fun Number.kiloNewtonMeters(): Measure<Torque> = this * Torque.kiloNewtonMeters

/** 질량과 가속도로 힘을 계산합니다. */
@JvmName("massTimesAccelerationToForce")
operator fun Measure<Mass>.times(other: Measure<Acceleration>): Measure<Force> =
    ((this `in` Mass.kilograms) * (other `in` MotionUnits.metersPerSecondSquared)) * Force.newtons

/** 가속도와 질량으로 힘을 계산합니다. */
@JvmName("accelerationTimesMassToForce")
operator fun Measure<Acceleration>.times(other: Measure<Mass>): Measure<Force> = other * this

/** 힘을 가속도로 나눠 질량을 계산합니다. */
@JvmName("forceDivAccelerationToMass")
operator fun Measure<Force>.div(other: Measure<Acceleration>): Measure<Mass> =
    ((this `in` Force.newtons) / (other `in` MotionUnits.metersPerSecondSquared)) * Mass.kilograms

/** 수직 모멘트암을 사용해 힘으로부터 토크를 계산합니다. */
fun Measure<Force>.torqueAt(perpendicularArm: Measure<Length>): Measure<Torque> =
    ((this `in` Force.newtons) * (perpendicularArm `in` Length.meters)) * Torque.newtonMeters
