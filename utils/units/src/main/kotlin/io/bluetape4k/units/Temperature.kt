package io.bluetape4k.units

import java.io.Serializable

fun temperatureOf(value: Number = 0.0, unit: TemperatureUnit = TemperatureUnit.KELVIN) =
    Temperature.of(value.toDouble(), unit)

fun <T: Number> T.temperatureBy(unit: TemperatureUnit): Temperature =
    temperatureOf(this, unit)

fun <T: Number> T.kelvin(): Temperature = this.temperatureBy(TemperatureUnit.KELVIN)
fun <T: Number> T.celsius(): Temperature = this.temperatureBy(TemperatureUnit.CELSIUS)
fun <T: Number> T.fahrenheit(): Temperature = this.temperatureBy(TemperatureUnit.FAHRENHEIT)

operator fun <T: Number> T.times(temperature: Temperature): Temperature = temperature.times(this)


/**
 * Convert Celsius to Fahrenheit
 */
fun Double.C2F(): Double = this * 1.8 + 32.0

/**
 * Convert Fahrenheit to Celsius
 */
fun Double.F2C(): Double = (this - 32.0) / 1.8

/**
 * 온도 단위
 */
enum class TemperatureUnit(val abbrName: String, val factor: Double) {
    KELVIN("K", 0.0),
    CELSIUS("°C", 273.15),
    FAHRENHEIT("°F", 459.67);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): TemperatureUnit {
            var upper = unitStr.uppercase()
            if (upper.endsWith("s")) {
                upper = upper.dropLast(1)
            }
            return TemperatureUnit.entries.find { it.abbrName == upper }
                ?: throw IllegalArgumentException("Unknown Temperature unit: $unitStr")
        }
    }
}

/**
 * 온도를 나타내는 클래스
 */
@JvmInline
value class Temperature(val value: Double = 0.0): Comparable<Temperature>, Serializable {

    /**
     * 덧셈은 [TemperatureUnit.KELVIN] 에서만 유효합니다.
     */
    operator fun plus(other: Temperature) = Temperature(value + other.value)

    /**
     * 뺄셈은 [TemperatureUnit.KELVIN] 에서만 유효합니다.
     */
    operator fun minus(other: Temperature) = Temperature(value - other.value)
    operator fun times(scalar: Number) = Temperature(value * scalar.toDouble())
    operator fun div(scalar: Number) = Temperature(value / scalar.toDouble())
    operator fun unaryMinus() = Temperature(-value)

    fun inKelvin(): Double = value
    fun inCelcius(): Double = value - TemperatureUnit.CELSIUS.factor
    fun inFahrenheit(): Double = value - TemperatureUnit.FAHRENHEIT.factor

    override fun compareTo(other: Temperature): Int = value.compareTo(other.value)

    fun toHuman(): String = toUnit(TemperatureUnit.KELVIN)
    fun toUnit(unit: TemperatureUnit): String =
        formatUnit(value - unit.factor, unit.abbrName)

    companion object {
        val ZERO = Temperature(0.0)
        val MIN_VALUE = Temperature(Double.MIN_VALUE)
        val MAX_VALUE = Temperature(Double.MAX_VALUE)
        val POSITIVE_INF = Temperature(Double.POSITIVE_INFINITY)
        val NEGATIVE_INF = Temperature(Double.NEGATIVE_INFINITY)
        val NaN = Temperature(Double.NaN)

        @JvmStatic
        fun of(temp: Double, unit: TemperatureUnit = TemperatureUnit.KELVIN): Temperature =
            Temperature(temp + unit.factor)

        @JvmStatic
        fun kelvin(value: Double) = of(value, TemperatureUnit.KELVIN)

        @JvmStatic
        fun celsius(value: Double) = of(value, TemperatureUnit.CELSIUS)

        @JvmStatic
        fun fahrenheit(value: Double) = of(value, TemperatureUnit.FAHRENHEIT)

        @JvmStatic
        fun parse(tempStr: String?): Temperature {
            if (tempStr.isNullOrBlank())
                return NaN

            try {
                val (temp, unit) = tempStr.split(" ", limit = 2)
                return of(temp.toDouble(), TemperatureUnit.parse(unit))
            } catch (e: Exception) {
                throw IllegalArgumentException("Unknown Temperature string. tempStr=$tempStr")
            }
        }
    }
}
