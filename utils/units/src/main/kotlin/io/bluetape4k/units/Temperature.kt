package io.bluetape4k.units

import io.bluetape4k.logging.KLogging

fun temperatureOf(
    value: Number = 0.0,
    unit: TemperatureUnit = TemperatureUnit.KELVIN,
) = Temperature(value.toDouble(), unit)

fun <T: Number> T.temperatureBy(unit: TemperatureUnit): Temperature = temperatureOf(this, unit)

fun <T: Number> T.kelvin(): Temperature = this.temperatureBy(TemperatureUnit.KELVIN)

fun <T: Number> T.celsius(): Temperature = this.temperatureBy(TemperatureUnit.CELSIUS)

fun <T: Number> T.fahrenheit(): Temperature = this.temperatureBy(TemperatureUnit.FAHRENHEIT)

operator fun <T: Number> T.times(temperature: Temperature): Temperature = temperature.times(this)

/**
 * Convert Celsius to Fahrenheit
 */
fun Double.c2F(): Double = this * 1.8 + 32.0

/**
 * Convert Fahrenheit to Celsius
 */
fun Double.f2C(): Double = (this - 32.0) / 1.8

/**
 * 온도 단위
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
enum class TemperatureUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {
    KELVIN("K", 0.0),
    CELSIUS("°C", 273.15),
    FAHRENHEIT("°F", 459.67),
    ;

    companion object {
        @JvmStatic
        fun parse(unitStr: String): TemperatureUnit {
            var upper = unitStr.uppercase()
            if (upper.endsWith("s")) {
                upper = upper.dropLast(1)
            }
            return TemperatureUnit.entries.find { it.unitName == upper }
                ?: throw IllegalArgumentException("Unknown Temperature unit: $unitStr")
        }
    }
}

/**
 * 온도를 나타내는 클래스
 *
 * 날짜 값은 Kelvin 단위로 낶부적으로 저장됩니다.
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
@JvmInline
value class Temperature(
    override val value: Double = 0.0,
): Measurable<TemperatureUnit> {
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

    override fun valueBy(unit: TemperatureUnit): Double =
        when (unit) {
            TemperatureUnit.KELVIN     -> value
            TemperatureUnit.CELSIUS    -> value - 273.15
            TemperatureUnit.FAHRENHEIT -> (value - 273.15) * 1.8 + 32.0
        }

    fun inKelvin(): Double = value

    fun inCelcius(): Double = value - 273.15

    fun inFahrenheit(): Double = (value - 273.15) * 1.8 + 32.0

    override fun convertTo(newUnit: TemperatureUnit): Temperature = Temperature(valueBy(newUnit), newUnit)

    override fun toHuman(): String = toHuman(TemperatureUnit.KELVIN)

    companion object: KLogging() {
        val ZERO = Temperature(0.0)
        val MIN_VALUE = Temperature(Double.MIN_VALUE)
        val MAX_VALUE = Temperature(Double.MAX_VALUE)
        val POSITIVE_INF = Temperature(Double.POSITIVE_INFINITY)
        val NEGATIVE_INF = Temperature(Double.NEGATIVE_INFINITY)
        val NaN = Temperature(Double.NaN)

        @JvmStatic
        operator fun invoke(
            temp: Double,
            unit: TemperatureUnit = TemperatureUnit.KELVIN,
        ): Temperature =
            when (unit) {
                TemperatureUnit.KELVIN     -> Temperature(temp)
                TemperatureUnit.CELSIUS    -> Temperature(temp + 273.15)
                TemperatureUnit.FAHRENHEIT -> Temperature((temp - 32.0) / 1.8 + 273.15)
            }

        @JvmStatic
        fun kelvin(value: Double) = invoke(value, TemperatureUnit.KELVIN)

        @JvmStatic
        fun celsius(value: Double) = invoke(value, TemperatureUnit.CELSIUS)

        @JvmStatic
        fun fahrenheit(value: Double) = invoke(value, TemperatureUnit.FAHRENHEIT)

        @JvmStatic
        fun parse(tempStr: String?): Temperature {
            if (tempStr.isNullOrBlank()) {
                return NaN
            }

            try {
                val (temp, unit) = tempStr.split(" ", limit = 2)
                return Temperature(temp.toDouble(), TemperatureUnit.parse(unit))
            } catch (e: Exception) {
                throw IllegalArgumentException("Unknown Temperature string. tempStr=$tempStr")
            }
        }
    }
}
