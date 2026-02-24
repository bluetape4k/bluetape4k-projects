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
 * `Measure<T>`를 `DOUBLE` 컬럼으로 저장하는 Exposed 컬럼 타입입니다.
 *
 * DB에는 [baseUnit] 기준의 수치값을 저장합니다.
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
        else -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Measure<T>): Any = value `in` baseUnit

    override fun nonNullValueToString(value: Measure<T>): String = (value `in` baseUnit).toString()
}

/**
 * 절대온도([Temperature])를 Kelvin `DOUBLE`로 저장하는 컬럼 타입입니다.
 */
class TemperatureColumnType: ColumnType<Temperature>() {
    private val delegate = DoubleColumnType()

    override fun sqlType(): String = delegate.sqlType()

    override fun valueFromDB(value: Any): Temperature? = when (value) {
        is Temperature -> value
        is Number -> Temperature.fromKelvin(value.toDouble())
        else -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: Temperature): Any = value.inKelvin()

    override fun nonNullValueToString(value: Temperature): String = value.inKelvin().toString()
}

/**
 * 온도차([TemperatureDelta])를 Kelvin delta `DOUBLE`로 저장하는 컬럼 타입입니다.
 */
class TemperatureDeltaColumnType: ColumnType<TemperatureDelta>() {
    private val delegate = DoubleColumnType()

    override fun sqlType(): String = delegate.sqlType()

    override fun valueFromDB(value: Any): TemperatureDelta? = when (value) {
        is TemperatureDelta -> value
        is Number -> TemperatureDelta(value.toDouble())
        else -> error("Unexpected value=$value, type=${value::class.qualifiedName}")
    }

    override fun notNullValueToDB(value: TemperatureDelta): Any = value.inKelvin()

    override fun nonNullValueToString(value: TemperatureDelta): String = value.inKelvin().toString()
}

/**
 * [Measure] 컬럼을 등록합니다.
 */
fun <T: Units> Table.measure(
    name: String,
    baseUnit: T,
): Column<Measure<T>> = registerColumn(
    name,
    MeasureColumnType(baseUnit) { base -> Measure(base, baseUnit) }
)

/** 길이 컬럼 (meter 기준) */
fun Table.length(name: String): Column<Measure<Length>> = measure(name, Length.meters)

/** 질량 컬럼 (kilogram 기준) */
fun Table.mass(name: String): Column<Measure<Mass>> = measure(name, Mass.kilograms)

/** 시간 컬럼 (second 기준) */
fun Table.time(name: String): Column<Measure<Time>> = measure(name, Time.seconds)

/** 면적 컬럼 (square meter 기준) */
fun Table.area(name: String): Column<Measure<Area>> = measure(name, Area.meters2)

/** 부피 컬럼 (cubic meter 기준) */
fun Table.volume(name: String): Column<Measure<Volume>> = measure(name, Volume.cubicMeters)

/** 각도 컬럼 (radian 기준) */
fun Table.angle(name: String): Column<Measure<Angle>> = measure(name, Angle.radians)

/** 압력 컬럼 (pascal 기준) */
fun Table.pressure(name: String): Column<Measure<Pressure>> = measure(name, Pressure.pascal)

/** 저장용량 컬럼 (byte 기준) */
fun Table.storage(name: String): Column<Measure<Storage>> = measure(name, Storage.bytes)

/** 디지털 크기 컬럼 (byte 기준) */
fun Table.binarySize(name: String): Column<Measure<BinarySize>> = measure(name, BinarySize.bytes)

/** 주파수 컬럼 (hertz 기준) */
fun Table.frequency(name: String): Column<Measure<Frequency>> = measure(name, Frequency.hertz)

/** 에너지 컬럼 (joule 기준) */
fun Table.energy(name: String): Column<Measure<Energy>> = measure(name, Energy.joules)

/** 전력 컬럼 (watt 기준) */
fun Table.power(name: String): Column<Measure<Power>> = measure(name, Power.watts)

/** 절대온도 컬럼 (kelvin 기준) */
fun Table.temperature(name: String): Column<Temperature> = registerColumn(name, TemperatureColumnType())

/** 온도차 컬럼 (kelvin delta 기준) */
fun Table.temperatureDelta(name: String): Column<TemperatureDelta> = registerColumn(name, TemperatureDeltaColumnType())
