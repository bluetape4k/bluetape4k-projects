package io.bluetape4k.measured

/**
 * 전류 단위를 나타냅니다.
 *
 * 기준 단위는 암페어([amps])이며 SI 접두어를 제공합니다.
 */
open class Current(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val amps: Current = Current("A")

        @JvmField
        val milliAmps: Current = Current("mA", 1.0e-3)

        @JvmField
        val kiloAmps: Current = Current("kA", 1.0e3)
    }
}

/**
 * 전하 단위를 나타냅니다.
 *
 * 기준 단위는 쿨롱([coulombs])입니다.
 */
open class Charge(
    suffix: String,
    ratio: Double = 1.0,
): Units(suffix, ratio) {
    companion object {
        @JvmField
        val coulombs: Charge = Charge("C")

        @JvmField
        val milliCoulombs: Charge = Charge("mC", 1.0e-3)

        @JvmField
        val microCoulombs: Charge = Charge("μC", 1.0e-6)
    }
}

/** 숫자를 암페어 단위 측정값으로 변환합니다. */
fun Number.amps(): Measure<Current> = this * Current.amps

/** 숫자를 밀리암페어 단위 측정값으로 변환합니다. */
fun Number.milliAmps(): Measure<Current> = this * Current.milliAmps

/** 숫자를 킬로암페어 단위 측정값으로 변환합니다. */
fun Number.kiloAmps(): Measure<Current> = this * Current.kiloAmps

/** 숫자를 쿨롱 단위 측정값으로 변환합니다. */
fun Number.coulombs(): Measure<Charge> = this * Charge.coulombs

/** 숫자를 밀리쿨롱 단위 측정값으로 변환합니다. */
fun Number.milliCoulombs(): Measure<Charge> = this * Charge.milliCoulombs

/** 숫자를 마이크로쿨롱 단위 측정값으로 변환합니다. */
fun Number.microCoulombs(): Measure<Charge> = this * Charge.microCoulombs

/** 전류와 시간을 곱해 전하를 계산합니다. */
@JvmName("currentTimesTimeToCharge")
operator fun Measure<Current>.times(other: Measure<Time>): Measure<Charge> =
    ((this `in` Current.amps) * (other `in` Time.seconds)) * Charge.coulombs

/** 시간을 전류와 곱해 전하를 계산합니다. */
@JvmName("timeTimesCurrentToCharge")
operator fun Measure<Time>.times(other: Measure<Current>): Measure<Charge> = other * this

/** 전하를 시간으로 나눠 전류를 계산합니다. */
@JvmName("chargeDivTimeToCurrent")
operator fun Measure<Charge>.div(other: Measure<Time>): Measure<Current> =
    ((this `in` Charge.coulombs) / (other `in` Time.seconds)) * Current.amps
