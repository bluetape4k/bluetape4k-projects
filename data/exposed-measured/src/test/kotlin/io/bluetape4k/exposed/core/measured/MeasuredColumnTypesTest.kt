package io.bluetape4k.exposed.core.measured

import io.bluetape4k.measured.Area
import io.bluetape4k.measured.Energy
import io.bluetape4k.measured.Length
import io.bluetape4k.measured.Measure
import io.bluetape4k.measured.Power
import io.bluetape4k.measured.Temperature
import io.bluetape4k.measured.centimeters
import io.bluetape4k.measured.celsius
import io.bluetape4k.measured.celsiusDelta
import io.bluetape4k.measured.kilometers2
import io.bluetape4k.measured.kiloWattHours
import io.bluetape4k.measured.meters
import io.bluetape4k.measured.watts
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeNear
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MeasuredColumnTypesTest {

    private object MeasureTable: IntIdTable("measured_column_type_test") {
        val length = measure("length", Length.meters)
        val area = measure("area", Area.meters2)
        val energy = measure("energy", Energy.joules)
        val power = measure("power", Power.watts)
        val temperature = temperature("temperature")
        val temperatureDelta = temperatureDelta("temperature_delta")
    }

    @Nested
    inner class Jdbc: AbstractExposedTest() {
        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `Measure, Temperature 컬럼은 모든 DB에서 round-trip 된다`(testDB: TestDB) {
            withTables(testDB, MeasureTable) {
                val insertedId = MeasureTable.insertAndGetId {
                    it[MeasureTable.length] = 150.centimeters()
                    it[MeasureTable.area] = 2.5.kilometers2()
                    it[MeasureTable.energy] = 1.kiloWattHours()
                    it[MeasureTable.power] = 250.watts()
                    it[MeasureTable.temperature] = 25.celsius()
                    it[MeasureTable.temperatureDelta] = 10.celsiusDelta()
                }

                val row = MeasureTable.selectAll()
                    .where { MeasureTable.id eq insertedId }
                    .single()

                (row[MeasureTable.length] `in` Length.meters).shouldBeNear(1.5, 1e-10)
                (row[MeasureTable.area] `in` Area.meters2).shouldBeNear(2_500_000.0, 1e-4)
                (row[MeasureTable.energy] `in` Energy.joules).shouldBeNear(3_600_000.0, 1e-5)
                (row[MeasureTable.power] `in` Power.watts).shouldBeNear(250.0, 1e-10)
                row[MeasureTable.temperature].inCelsius().shouldBeNear(25.0, 1e-10)
                row[MeasureTable.temperatureDelta].inCelsius().shouldBeNear(10.0, 1e-10)
            }
        }
    }

    @org.junit.jupiter.api.Test
    fun `MeasureColumnType 는 base unit 값으로 직렬화한다`() {
        val columnType = MeasureColumnType(Length.meters) { Measure(it, Length.meters) }
        val encoded = columnType.notNullValueToDB(1.5.meters()) as Double
        encoded.shouldBeNear(1.5, 1e-10)
    }

    @org.junit.jupiter.api.Test
    fun `MeasureColumnType 는 숫자 DB 값을 Measure 로 역직렬화한다`() {
        val columnType = MeasureColumnType(Area.meters2) { Measure(it, Area.meters2) }
        val decoded = columnType.valueFromDB(25.0)!!
        (decoded `in` Area.meters2).shouldBeNear(25.0, 1e-10)
    }

    @org.junit.jupiter.api.Test
    fun `TemperatureColumnType 는 Kelvin 기준으로 변환한다`() {
        val columnType = TemperatureColumnType()
        val encoded = columnType.notNullValueToDB(25.celsius()) as Double
        encoded.shouldBeNear(298.15, 1e-10)

        val decoded = columnType.valueFromDB(encoded)!!
        decoded.inCelsius().shouldBeNear(25.0, 1e-10)
    }
}
