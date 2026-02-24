package io.bluetape4k.measured

/**
 * 절대 온도 단위를 정의합니다.
 */
enum class TemperatureUnit(val suffix: String) {
    KELVIN("K"),
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

/**
 * 온도 차이를 Kelvin 기준으로 표현합니다.
 */
@JvmInline
value class TemperatureDelta(val kelvin: Double) {
    /** Kelvin 온도 차이입니다. */
    fun inKelvin(): Double = kelvin

    /** Celsius 온도 차이입니다. */
    fun inCelsius(): Double = kelvin

    /** Fahrenheit 온도 차이입니다. */
    fun inFahrenheit(): Double = kelvin * 9.0 / 5.0

    /** 사람이 읽기 쉬운 형식으로 온도 차이를 반환합니다. */
    fun toHuman(unit: TemperatureUnit = TemperatureUnit.CELSIUS): String =
        when (unit) {
            TemperatureUnit.KELVIN     -> "${inKelvin()} ${TemperatureUnit.KELVIN.suffix}"
            TemperatureUnit.CELSIUS    -> "${inCelsius()} ${TemperatureUnit.CELSIUS.suffix}"
            TemperatureUnit.FAHRENHEIT -> "${inFahrenheit()} ${TemperatureUnit.FAHRENHEIT.suffix}"
        }
}

/**
 * 절대 온도를 Kelvin 기준으로 저장합니다.
 */
class Temperature private constructor(private val kelvin: Double): Comparable<Temperature> {
    /** Kelvin 온도로 변환합니다. */
    fun inKelvin(): Double = kelvin

    /** Celsius 온도로 변환합니다. */
    fun inCelsius(): Double = kelvin - 273.15

    /** Fahrenheit 온도로 변환합니다. */
    fun inFahrenheit(): Double = (kelvin - 273.15) * 9.0 / 5.0 + 32.0

    /** 지정 단위로 사람이 읽기 쉬운 문자열을 반환합니다. */
    fun toHuman(unit: TemperatureUnit = TemperatureUnit.CELSIUS): String =
        when (unit) {
            TemperatureUnit.KELVIN     -> "${inKelvin()} ${TemperatureUnit.KELVIN.suffix}"
            TemperatureUnit.CELSIUS    -> "${inCelsius()} ${TemperatureUnit.CELSIUS.suffix}"
            TemperatureUnit.FAHRENHEIT -> "${inFahrenheit()} ${TemperatureUnit.FAHRENHEIT.suffix}"
        }

    /** 온도 차이를 더합니다. */
    operator fun plus(delta: TemperatureDelta): Temperature = fromKelvin(kelvin + delta.kelvin)

    /** 온도 차이를 뺍니다. */
    operator fun minus(delta: TemperatureDelta): Temperature = fromKelvin(kelvin - delta.kelvin)

    /** 두 절대 온도의 차이를 계산합니다. */
    operator fun minus(other: Temperature): TemperatureDelta = TemperatureDelta(kelvin - other.kelvin)

    override fun compareTo(other: Temperature): Int = kelvin.compareTo(other.kelvin)

    override fun toString(): String = toHuman(TemperatureUnit.CELSIUS)

    override fun equals(other: Any?): Boolean = other is Temperature && kelvin == other.kelvin

    override fun hashCode(): Int = kelvin.hashCode()

    companion object {
        /** Kelvin 값으로 절대 온도를 생성합니다. */
        @JvmStatic
        fun fromKelvin(value: Double): Temperature = Temperature(value)

        /** Celsius 값으로 절대 온도를 생성합니다. */
        @JvmStatic
        fun fromCelsius(value: Double): Temperature = Temperature(value + 273.15)

        /** Fahrenheit 값으로 절대 온도를 생성합니다. */
        @JvmStatic
        fun fromFahrenheit(value: Double): Temperature = Temperature((value - 32.0) * 5.0 / 9.0 + 273.15)
    }
}

/**
 * 숫자를 Kelvin 절대 온도로 변환합니다.
 */
fun Number.kelvin(): Temperature = Temperature.fromKelvin(this.toDouble())

/**
 * 숫자를 Celsius 절대 온도로 변환합니다.
 */
fun Number.celsius(): Temperature = Temperature.fromCelsius(this.toDouble())

/**
 * 숫자를 Fahrenheit 절대 온도로 변환합니다.
 */
fun Number.fahrenheit(): Temperature = Temperature.fromFahrenheit(this.toDouble())

/**
 * 숫자를 Kelvin 온도 차이로 변환합니다.
 */
fun Number.kelvinDelta(): TemperatureDelta = TemperatureDelta(this.toDouble())

/**
 * 숫자를 Celsius 온도 차이로 변환합니다.
 */
fun Number.celsiusDelta(): TemperatureDelta = TemperatureDelta(this.toDouble())

/**
 * 숫자를 Fahrenheit 온도 차이로 변환합니다.
 */
fun Number.fahrenheitDelta(): TemperatureDelta = TemperatureDelta(this.toDouble() * 5.0 / 9.0)
