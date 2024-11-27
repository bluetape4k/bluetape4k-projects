package io.bluetape4k.units

import java.io.Serializable

enum class PressureUnit {
    PASCAL,
    BAR,
    ATMOSPHERE,
    TORR,
    POUND_PER_SQUARE_INCH
}

@JvmInline
value class Pressure(val value: Double): Comparable<Pressure>, Serializable {

    fun convertTo(newUnit: PressureUnit): Pressure {
        return when (newUnit) {
            PressureUnit.PASCAL                -> Pressure(value)
            PressureUnit.BAR                   -> Pressure(value / 100000)
            PressureUnit.ATMOSPHERE            -> Pressure(value / 101325)
            PressureUnit.TORR                  -> Pressure(value / 133.322)
            PressureUnit.POUND_PER_SQUARE_INCH -> Pressure(value / 6894.757)
        }
    }

    override operator fun compareTo(other: Pressure): Int = value.compareTo(other.value)
}
