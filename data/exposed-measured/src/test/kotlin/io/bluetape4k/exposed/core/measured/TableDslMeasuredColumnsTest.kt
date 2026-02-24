package io.bluetape4k.exposed.core.measured

import io.bluetape4k.measured.Angle
import io.bluetape4k.measured.Area
import io.bluetape4k.measured.BinarySize
import io.bluetape4k.measured.Energy
import io.bluetape4k.measured.Frequency
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Mass
import io.bluetape4k.measured.Power
import io.bluetape4k.measured.Pressure
import io.bluetape4k.measured.Storage
import io.bluetape4k.measured.Temperature
import io.bluetape4k.measured.TemperatureDelta
import io.bluetape4k.measured.Time
import io.bluetape4k.measured.Units
import io.bluetape4k.measured.Volume
import io.bluetape4k.measured.atm
import io.bluetape4k.measured.binaryBytes
import io.bluetape4k.measured.celsius
import io.bluetape4k.measured.celsiusDelta
import io.bluetape4k.measured.cubicMeters
import io.bluetape4k.measured.degrees
import io.bluetape4k.measured.gbytes
import io.bluetape4k.measured.gigaHertz
import io.bluetape4k.measured.hours
import io.bluetape4k.measured.kilometers2
import io.bluetape4k.measured.kiloWattHours
import io.bluetape4k.measured.kilograms
import io.bluetape4k.measured.kiloWatts
import io.bluetape4k.measured.meters
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TableDslMeasuredColumnsTest {

    private object SampleTable: Table("sample_measured") {
        val lengthCol = length("length")
        val massCol = mass("mass")
        val timeCol = time("time")
        val areaCol = area("area")
        val volumeCol = volume("volume")
        val angleCol = angle("angle")
        val pressureCol = pressure("pressure")
        val storageCol = storage("storage")
        val binarySizeCol = binarySize("binary_size")
        val frequencyCol = frequency("frequency")
        val energyCol = energy("energy")
        val powerCol = power("power")
        val tempCol = temperature("temp")
        val tempDeltaCol = temperatureDelta("temp_delta")
    }

    @Test
    fun `typed DSL 이 올바른 measured 타입으로 컬럼을 생성한다`() {
        assertMeasureBaseUnit(SampleTable.lengthCol.columnType, Length.meters)
        assertMeasureBaseUnit(SampleTable.massCol.columnType, Mass.kilograms)
        assertMeasureBaseUnit(SampleTable.timeCol.columnType, Time.seconds)
        assertMeasureBaseUnit(SampleTable.areaCol.columnType, Area.meters2)
        assertMeasureBaseUnit(SampleTable.volumeCol.columnType, Volume.cubicMeters)
        assertMeasureBaseUnit(SampleTable.angleCol.columnType, Angle.radians)
        assertMeasureBaseUnit(SampleTable.pressureCol.columnType, Pressure.pascal)
        assertMeasureBaseUnit(SampleTable.storageCol.columnType, Storage.bytes)
        assertMeasureBaseUnit(SampleTable.binarySizeCol.columnType, BinarySize.bytes)
        assertMeasureBaseUnit(SampleTable.frequencyCol.columnType, Frequency.hertz)
        assertMeasureBaseUnit(SampleTable.energyCol.columnType, Energy.joules)
        assertMeasureBaseUnit(SampleTable.powerCol.columnType, Power.watts)

        (SampleTable.tempCol.columnType is TemperatureColumnType) shouldBeEqualTo true
        (SampleTable.tempDeltaCol.columnType is TemperatureDeltaColumnType) shouldBeEqualTo true
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Units> assertMeasureBaseUnit(columnType: Any, expected: T) {
        (columnType is MeasureColumnType<*>) shouldBeEqualTo true
        val typed = columnType as MeasureColumnType<T>
        val decoded = typed.valueFromDB(1.0)!!
        (decoded.units == expected) shouldBeEqualTo true
    }

    @Nested
    inner class Jdbc: AbstractExposedTest() {
        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `typed DSL 컬럼은 모든 DB에서 insert-select 가 가능하다`(testDB: TestDB) {
            withTables(testDB, SampleTable) {
                SampleTable.insert {
                    it[SampleTable.lengthCol] = 1.5.meters()
                    it[SampleTable.massCol] = 72.kilograms()
                    it[SampleTable.timeCol] = 2.hours()
                    it[SampleTable.areaCol] = 3.0.kilometers2()
                    it[SampleTable.volumeCol] = 250_000_000.0.cubicMeters()
                    it[SampleTable.angleCol] = 90.degrees()
                    it[SampleTable.pressureCol] = 1.atm()
                    it[SampleTable.storageCol] = 128.gbytes()
                    it[SampleTable.binarySizeCol] = 2048.binaryBytes()
                    it[SampleTable.frequencyCol] = 2.4.gigaHertz()
                    it[SampleTable.energyCol] = 1.kiloWattHours()
                    it[SampleTable.powerCol] = 2.2.kiloWatts()
                    it[SampleTable.tempCol] = 24.celsius()
                    it[SampleTable.tempDeltaCol] = 7.celsiusDelta()
                }
                commit()

                val row = SampleTable.selectAll().single()
                (row[SampleTable.lengthCol] `in` Length.meters).shouldBeNear(1.5, 1e-10)
                (row[SampleTable.massCol] `in` Mass.kilograms).shouldBeNear(72.0, 1e-10)
                (row[SampleTable.timeCol] `in` Time.seconds).shouldBeNear(7200.0, 1e-10)
                (row[SampleTable.areaCol] `in` Area.meters2).shouldBeNear(3_000_000.0, 1e-4)
                (row[SampleTable.volumeCol] `in` Volume.cubicMeters).shouldBeNear(250_000_000.0, 1e-3)
                (row[SampleTable.angleCol] `in` Angle.radians).shouldBeNear(Math.PI / 2.0, 1e-10)
                (row[SampleTable.pressureCol] `in` Pressure.pascal).shouldBeNear(101_325.0, 1e-2)
                (row[SampleTable.storageCol] `in` Storage.bytes).shouldBeNear(137_438_953_472.0, 1e-2)
                (row[SampleTable.binarySizeCol] `in` BinarySize.bytes).shouldBeNear(2048.0, 1e-10)
                (row[SampleTable.frequencyCol] `in` Frequency.hertz).shouldBeNear(2_400_000_000.0, 1e-2)
                (row[SampleTable.energyCol] `in` Energy.joules).shouldBeNear(3_600_000.0, 1e-5)
                (row[SampleTable.powerCol] `in` Power.watts).shouldBeNear(2200.0, 1e-10)
                row[SampleTable.tempCol].inCelsius().shouldBeNear(24.0, 1e-10)
                row[SampleTable.tempDeltaCol].inCelsius().shouldBeNear(7.0, 1e-10)
            }
        }
    }
}
