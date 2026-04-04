package io.bluetape4k.measured

/**
 * 절대 온도 단위를 정의합니다.
 *
 * ## 동작/계약
 * - [KELVIN], [CELSIUS], [FAHRENHEIT] 3개 표현 단위를 제공합니다.
 * - 단위 enum 자체는 상태가 없는 불변 상수입니다.
 *
 * ```kotlin
 * val unit = TemperatureUnit.CELSIUS
 * // unit.suffix == "°C"
 * ```
 */
enum class TemperatureUnit(val suffix: String) {
    KELVIN("K"),
    CELSIUS("°C"),
    FAHRENHEIT("°F"),
}

/**
 * 온도 차이를 Kelvin 기준으로 표현합니다.
 *
 * ## 동작/계약
 * - 내부 저장값은 항상 Kelvin 차이입니다.
 * - `Celsius` 차이는 Kelvin과 동일 크기, `Fahrenheit` 차이는 `9/5` 배율을 적용합니다.
 *
 * ```kotlin
 * val delta = 10.celsiusDelta()
 * // delta.inKelvin() == 10.0
 * // delta.inFahrenheit() == 18.0
 * ```
 */
@JvmInline
value class TemperatureDelta(val kelvin: Double) {
    /**
     * Kelvin 온도 차이입니다.
     *
     * ```kotlin
     * val delta = 10.celsiusDelta()
     * // delta.inKelvin() == 10.0
     * ```
     */
    fun inKelvin(): Double = kelvin

    /**
     * Celsius 온도 차이입니다.
     *
     * ```kotlin
     * val delta = 10.kelvinDelta()
     * // delta.inCelsius() == 10.0
     * ```
     */
    fun inCelsius(): Double = kelvin

    /**
     * Fahrenheit 온도 차이입니다.
     *
     * ```kotlin
     * val delta = 10.celsiusDelta()
     * // delta.inFahrenheit() == 18.0
     * ```
     */
    fun inFahrenheit(): Double = kelvin * 9.0 / 5.0

    /**
     * 사람이 읽기 쉬운 형식으로 온도 차이를 반환합니다.
     *
     * ```kotlin
     * val delta = 5.celsiusDelta()
     * // delta.toHuman() == "5.0 °C"
     * // delta.toHuman(TemperatureUnit.KELVIN) == "5.0 K"
     * ```
     */
    fun toHuman(unit: TemperatureUnit = TemperatureUnit.CELSIUS): String =
        when (unit) {
            TemperatureUnit.KELVIN     -> "${inKelvin()} ${TemperatureUnit.KELVIN.suffix}"
            TemperatureUnit.CELSIUS    -> "${inCelsius()} ${TemperatureUnit.CELSIUS.suffix}"
            TemperatureUnit.FAHRENHEIT -> "${inFahrenheit()} ${TemperatureUnit.FAHRENHEIT.suffix}"
        }
}

/**
 * 절대 온도를 Kelvin 기준으로 저장합니다.
 *
 * ## 동작/계약
 * - 내부 값은 절대온도 Kelvin으로 보관합니다.
 * - 온도 +/− 온도차는 새 [Temperature]를 반환합니다.
 * - 절대온도끼리 뺄셈은 [TemperatureDelta]를 반환합니다.
 *
 * ```kotlin
 * val base = 25.celsius()
 * val raised = base + 10.celsiusDelta()
 * // raised.inCelsius() == 35.0
 * // (raised - base).inCelsius() == 10.0
 * ```
 */
class Temperature private constructor(private val kelvin: Double): Comparable<Temperature> {
    /**
     * Kelvin 온도로 변환합니다.
     *
     * ```kotlin
     * val t = 25.celsius()
     * // t.inKelvin() == 298.15
     * ```
     */
    fun inKelvin(): Double = kelvin

    /**
     * Celsius 온도로 변환합니다.
     *
     * ```kotlin
     * val t = 298.15.kelvin()
     * // t.inCelsius() == 25.0
     * ```
     */
    fun inCelsius(): Double = kelvin - 273.15

    /**
     * Fahrenheit 온도로 변환합니다.
     *
     * ```kotlin
     * val t = 100.celsius()
     * // t.inFahrenheit() == 212.0
     * ```
     */
    fun inFahrenheit(): Double = (kelvin - 273.15) * 9.0 / 5.0 + 32.0

    /**
     * 지정 단위로 사람이 읽기 쉬운 문자열을 반환합니다.
     *
     * ```kotlin
     * val t = 25.celsius()
     * // t.toHuman() == "25.0 °C"
     * // t.toHuman(TemperatureUnit.KELVIN) == "298.15 K"
     * ```
     */
    fun toHuman(unit: TemperatureUnit = TemperatureUnit.CELSIUS): String =
        when (unit) {
            TemperatureUnit.KELVIN     -> "${inKelvin()} ${TemperatureUnit.KELVIN.suffix}"
            TemperatureUnit.CELSIUS    -> "${inCelsius()} ${TemperatureUnit.CELSIUS.suffix}"
            TemperatureUnit.FAHRENHEIT -> "${inFahrenheit()} ${TemperatureUnit.FAHRENHEIT.suffix}"
        }

    /**
     * 온도 차이를 더합니다.
     *
     * ```kotlin
     * val t = 20.celsius() + 5.celsiusDelta()
     * // t.inCelsius() == 25.0
     * ```
     */
    operator fun plus(delta: TemperatureDelta): Temperature = fromKelvin(kelvin + delta.kelvin)

    /**
     * 온도 차이를 뺍니다.
     *
     * ```kotlin
     * val t = 20.celsius() - 5.celsiusDelta()
     * // t.inCelsius() == 15.0
     * ```
     */
    operator fun minus(delta: TemperatureDelta): Temperature = fromKelvin(kelvin - delta.kelvin)

    /**
     * 두 절대 온도의 차이를 계산합니다.
     *
     * ```kotlin
     * val delta = 30.celsius() - 20.celsius()
     * // delta.inCelsius() == 10.0
     * ```
     */
    operator fun minus(other: Temperature): TemperatureDelta = TemperatureDelta(kelvin - other.kelvin)

    override fun compareTo(other: Temperature): Int = kelvin.compareTo(other.kelvin)

    override fun toString(): String = toHuman(TemperatureUnit.CELSIUS)

    override fun equals(other: Any?): Boolean = other is Temperature && kelvin == other.kelvin

    override fun hashCode(): Int = kelvin.hashCode()

    companion object {
        /**
         * Kelvin 값으로 절대 온도를 생성합니다.
         *
         * ```kotlin
         * val t = Temperature.fromKelvin(273.15)
         * // t.inCelsius() == 0.0
         * ```
         */
        @JvmStatic
        fun fromKelvin(value: Double): Temperature = Temperature(value)

        /**
         * Celsius 값으로 절대 온도를 생성합니다.
         *
         * ```kotlin
         * val t = Temperature.fromCelsius(100.0)
         * // t.inKelvin() == 373.15
         * ```
         */
        @JvmStatic
        fun fromCelsius(value: Double): Temperature = Temperature(value + 273.15)

        /**
         * Fahrenheit 값으로 절대 온도를 생성합니다.
         *
         * ```kotlin
         * val t = Temperature.fromFahrenheit(212.0)
         * // t.inCelsius() == 100.0
         * ```
         */
        @JvmStatic
        fun fromFahrenheit(value: Double): Temperature = Temperature((value - 32.0) * 5.0 / 9.0 + 273.15)
    }
}

/**
 * 숫자를 Kelvin 절대 온도로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 숫자를 Kelvin 절대온도로 그대로 해석합니다.
 *
 * ```kotlin
 * val t = 298.15.kelvin()
 * // t.inCelsius() == 25.0
 * ```
 */
fun Number.kelvin(): Temperature = Temperature.fromKelvin(this.toDouble())

/**
 * 숫자를 Celsius 절대 온도로 변환합니다.
 *
 * ## 동작/계약
 * - `°C + 273.15`로 Kelvin 내부값을 계산합니다.
 *
 * ```kotlin
 * val t = 0.celsius()
 * // t.inKelvin() == 273.15
 * ```
 */
fun Number.celsius(): Temperature = Temperature.fromCelsius(this.toDouble())

/**
 * 숫자를 Fahrenheit 절대 온도로 변환합니다.
 *
 * ## 동작/계약
 * - 화씨를 섭씨/켈빈으로 변환해 내부값을 계산합니다.
 *
 * ```kotlin
 * val t = 32.fahrenheit()
 * // t.inCelsius() == 0.0
 * ```
 */
fun Number.fahrenheit(): Temperature = Temperature.fromFahrenheit(this.toDouble())

/**
 * 숫자를 Kelvin 온도 차이로 변환합니다.
 *
 * ```kotlin
 * val delta = 10.kelvinDelta()
 * // delta.inKelvin() == 10.0
 * // delta.inCelsius() == 10.0
 * ```
 */
fun Number.kelvinDelta(): TemperatureDelta = TemperatureDelta(this.toDouble())

/**
 * 숫자를 Celsius 온도 차이로 변환합니다.
 *
 * ```kotlin
 * val delta = 5.celsiusDelta()
 * // delta.inKelvin() == 5.0
 * // delta.inFahrenheit() == 9.0
 * ```
 */
fun Number.celsiusDelta(): TemperatureDelta = TemperatureDelta(this.toDouble())

/**
 * 숫자를 Fahrenheit 온도 차이로 변환합니다.
 *
 * ```kotlin
 * val delta = 18.fahrenheitDelta()
 * // delta.inCelsius() == 10.0
 * // delta.inKelvin() == 10.0
 * ```
 */
fun Number.fahrenheitDelta(): TemperatureDelta = TemperatureDelta(this.toDouble() * 5.0 / 9.0)
