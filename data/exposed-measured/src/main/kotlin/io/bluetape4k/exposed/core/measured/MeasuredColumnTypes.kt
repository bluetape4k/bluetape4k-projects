package io.bluetape4k.exposed.core.measured

import io.bluetape4k.measured.Angle
import io.bluetape4k.measured.Area
import io.bluetape4k.measured.BinarySize
import io.bluetape4k.measured.Energy
import io.bluetape4k.measured.Frequency
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Mass
import io.bluetape4k.measured.Measure
import io.bluetape4k.measured.Power
import io.bluetape4k.measured.Pressure
import io.bluetape4k.measured.Storage
import io.bluetape4k.measured.Temperature
import io.bluetape4k.measured.TemperatureDelta
import io.bluetape4k.measured.Time
import io.bluetape4k.measured.Units
import io.bluetape4k.measured.Volume
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * [Measure]를 `DOUBLE` 컬럼으로 매핑하는 Exposed 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB에는 항상 [baseUnit] 기준의 `Double` 값으로 저장합니다.
 * - `Number` 값을 읽으면 [fromBaseValue]로 [Measure]를 복원합니다.
 * - 지원하지 않는 DB 타입이 들어오면 `error(...)`로 예외가 발생합니다.
 *
 * ```kotlin
 * val type = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
 * val db = type.notNullValueToDB(1500.meters())
 * // db == 1500.0
 * ```
 */
class MeasureColumnType<T: Units>(
    private val baseUnit: T,
    private val fromBaseValue: (Double) -> Measure<T>,
): ColumnType<Measure<T>>() {
    private val delegate = DoubleColumnType()

    override fun sqlType(): String = delegate.sqlType()

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): Measure<T>? = when (value) {
        is Measure<*> -> value as Measure<T>
        is Number -> fromBaseValue(value.toDouble())
        else      -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Measure<T>): Any = value `in` baseUnit

    override fun nonNullValueToString(value: Measure<T>): String = (value `in` baseUnit).toString()
}

/**
 * [Temperature]를 Kelvin `DOUBLE` 컬럼으로 매핑합니다.
 *
 * ## 동작/계약
 * - 저장 시 `inKelvin()` 값을 사용합니다.
 * - 조회 시 `Number`를 `Temperature.fromKelvin`으로 복원합니다.
 * - 지원하지 않는 DB 타입이면 `error(...)` 예외가 발생합니다.
 *
 * ```kotlin
 * val type = TemperatureColumnType()
 * val db = type.notNullValueToDB(25.celsius())
 * // db == 298.15
 * ```
 */
class TemperatureColumnType: ColumnType<Temperature>() {
    private val delegate = DoubleColumnType()

    override fun sqlType(): String = delegate.sqlType()

    override fun valueFromDB(value: Any): Temperature? = when (value) {
        is Temperature -> value
        is Number -> Temperature.fromKelvin(value.toDouble())
        else      -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Temperature): Any = value.inKelvin()

    override fun nonNullValueToString(value: Temperature): String = value.inKelvin().toString()
}

/**
 * [TemperatureDelta]를 Kelvin delta `DOUBLE` 컬럼으로 매핑합니다.
 *
 * ## 동작/계약
 * - 저장 시 `inKelvin()` 값을 사용합니다.
 * - 조회 시 `Number`를 `TemperatureDelta`로 복원합니다.
 * - 지원하지 않는 DB 타입이면 `error(...)` 예외가 발생합니다.
 *
 * ```kotlin
 * val type = TemperatureDeltaColumnType()
 * val db = type.notNullValueToDB(10.celsiusDelta())
 * // db == 10.0
 * ```
 */
class TemperatureDeltaColumnType: ColumnType<TemperatureDelta>() {
    private val delegate = DoubleColumnType()

    override fun sqlType(): String = delegate.sqlType()

    override fun valueFromDB(value: Any): TemperatureDelta? = when (value) {
        is TemperatureDelta -> value
        is Number -> TemperatureDelta(value.toDouble())
        else      -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: TemperatureDelta): Any = value.inKelvin()

    override fun nonNullValueToString(value: TemperatureDelta): String = value.inKelvin().toString()
}

/**
 * [Measure] 타입 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장/조회 기준 단위는 [baseUnit]입니다.
 * - 내부적으로 [MeasureColumnType]을 사용해 `DOUBLE` 컬럼으로 매핑합니다.
 *
 * ```kotlin
 * val distance = measure("distance", Length.meters)
 * // distance.columnType.sqlType() == "DOUBLE"
 * ```
 */
fun <T: Units> Table.measure(
    name: String,
    baseUnit: T,
): Column<Measure<T>> = registerColumn(
    name,
    MeasureColumnType(baseUnit) { base -> Measure(base, baseUnit) }
)

/**
 * 길이 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `meter`입니다.
 *
 * ```kotlin
 * val col = length("distance")
 * // DB value == meter 기준 Double
 * ```
 */
fun Table.length(name: String): Column<Measure<Length>> = measure(name, Length.meters)

/**
 * 질량 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `kilogram`입니다.
 */
fun Table.mass(name: String): Column<Measure<Mass>> = measure(name, Mass.kilograms)

/**
 * 시간 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `second`입니다.
 */
fun Table.time(name: String): Column<Measure<Time>> = measure(name, Time.seconds)

/**
 * 면적 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `square meter`입니다.
 */
fun Table.area(name: String): Column<Measure<Area>> = measure(name, Area.meters2)

/**
 * 부피 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `cubic meter`입니다.
 */
fun Table.volume(name: String): Column<Measure<Volume>> = measure(name, Volume.cubicMeters)

/**
 * 각도 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `radian`입니다.
 */
fun Table.angle(name: String): Column<Measure<Angle>> = measure(name, Angle.radians)

/**
 * 압력 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `pascal`입니다.
 */
fun Table.pressure(name: String): Column<Measure<Pressure>> = measure(name, Pressure.pascal)

/**
 * 저장용량 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `byte`입니다.
 */
fun Table.storage(name: String): Column<Measure<Storage>> = measure(name, Storage.bytes)

/**
 * 디지털 크기 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `byte`입니다.
 */
fun Table.binarySize(name: String): Column<Measure<BinarySize>> = measure(name, BinarySize.bytes)

/**
 * 주파수 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `hertz`입니다.
 */
fun Table.frequency(name: String): Column<Measure<Frequency>> = measure(name, Frequency.hertz)

/**
 * 에너지 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `joule`입니다.
 */
fun Table.energy(name: String): Column<Measure<Energy>> = measure(name, Energy.joules)

/**
 * 전력 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 기준 단위는 `watt`입니다.
 */
fun Table.power(name: String): Column<Measure<Power>> = measure(name, Power.watts)

/**
 * 절대온도 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - Kelvin `DOUBLE`로 저장/조회합니다.
 *
 * ```kotlin
 * val col = temperature("temp")
 * // DB value == kelvin Double
 * ```
 */
fun Table.temperature(name: String): Column<Temperature> = registerColumn(name, TemperatureColumnType())

/**
 * 온도차 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - Kelvin delta `DOUBLE`로 저장/조회합니다.
 */
fun Table.temperatureDelta(name: String): Column<TemperatureDelta> = registerColumn(name, TemperatureDeltaColumnType())
