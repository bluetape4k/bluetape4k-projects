package io.bluetape4k.measured

import kotlin.math.abs

/** 알려진 단위 계열의 사람이 읽기 쉬운 후보 단위를 반환합니다. */
private fun Units.baseHumanCandidates(): List<Units>? = when (this) {
    is Length     -> listOf(Length.millimeters, Length.centimeters, Length.meters, Length.kilometers)
    is Mass       -> listOf(Mass.grams, Mass.kilograms, Mass.tons)
    is Time       -> listOf(Time.milliseconds, Time.seconds, Time.minutes, Time.hours)
    is Area       -> listOf(Area.millimeters2, Area.centimeters2, Area.meters2, Area.kilometers2)
    is Volume     -> listOf(
        Volume.cubicMillimeters,
        Volume.cubicCentimeters,
        Volume.milliliters,
        Volume.liters,
        Volume.cubicMeters,
    )

    is Storage    -> listOf(
        Storage.bytes,
        Storage.kiloBytes,
        Storage.megaBytes,
        Storage.gigaBytes,
        Storage.teraBytes,
        Storage.petaBytes,
    )

    is BinarySize -> listOf(
        BinarySize.bits,
        BinarySize.bytes,
        BinarySize.kiloBytes,
        BinarySize.megaBytes,
        BinarySize.gigaBytes,
        BinarySize.teraBytes,
        BinarySize.petaBytes,
    )

    is Frequency  -> listOf(Frequency.hertz, Frequency.kiloHertz, Frequency.megaHertz, Frequency.gigaHertz)
    is Energy     -> listOf(
        Energy.joules,
        Energy.kiloJoules,
        Energy.megaJoules,
        Energy.wattHours,
        Energy.kiloWattHours,
    )

    is Power      -> listOf(Power.milliWatts, Power.watts, Power.kiloWatts, Power.megaWatts, Power.gigaWatts)
    is Pressure   -> listOf(
        Pressure.pascal,
        Pressure.hectoPascal,
        Pressure.kiloPascal,
        Pressure.megaPascal,
        Pressure.gigaPascal,
        Pressure.bar,
        Pressure.atmosphere,
        Pressure.psi,
    )

    else         -> null
}

/** 새로 추가된 단위 계열의 사람이 읽기 쉬운 후보 단위를 반환합니다. */
private fun Units.extendedHumanCandidates(): List<Units>? = when (this) {
    is DataRate   -> listOf(
        DataRate.bytesPerSecond,
        DataRate.kiloBytesPerSecond,
        DataRate.megaBytesPerSecond,
        DataRate.gigaBytesPerSecond,
    )

    is Current    -> listOf(Current.milliAmps, Current.amps, Current.kiloAmps)
    is Charge     -> listOf(Charge.microCoulombs, Charge.milliCoulombs, Charge.coulombs)
    is Voltage    -> listOf(Voltage.milliVolts, Voltage.volts, Voltage.kiloVolts)
    is Resistance -> listOf(Resistance.milliOhms, Resistance.ohms, Resistance.kiloOhms, Resistance.megaOhms)
    is Force      -> listOf(Force.milliNewtons, Force.newtons, Force.kiloNewtons, Force.megaNewtons)
    is Torque     -> listOf(Torque.newtonMeters, Torque.kiloNewtonMeters)
    else          -> null
}

/** 측정값의 기본 사람이 읽기 쉬운 표현을 계산합니다. */
internal fun <T: Units> formatMeasureHuman(measure: Measure<T>): String {
    val candidates = measure.units.baseHumanCandidates() ?: measure.units.extendedHumanCandidates()
    if (candidates != null) {
        @Suppress("UNCHECKED_CAST")
        return measure.toHumanBy(candidates as List<T>)
    }

    return when (measure.units) {
        is Angle -> {
            val degree = (((measure as Measure<Angle>) `in` Angle.degrees) % 360.0 + 360.0) % 360.0
            formatHuman(degree, Angle.degrees)
        }

        else -> formatHuman(measure.amount, measure.units)
    }
}

private fun <T: Units> Measure<T>.toHumanBy(candidates: List<T>): String {
    if (candidates.isEmpty()) return formatHuman(amount, units)

    val sorted = candidates.sortedBy { it.ratio }
    val best = sorted.lastOrNull { abs(this `in` it) >= 1.0 } ?: sorted.first()
    return formatHuman(this `in` best, best)
}
