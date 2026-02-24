@file:Suppress("DEPRECATION")

package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

@RandomizedTest
class UnitsCompatibilityTest {
    @Test
    fun `길이 라운드트립 변환이 동작한다`() {
        val legacy = io.bluetape4k.units.Length(1.5, io.bluetape4k.units.LengthUnit.KILOMETER)
        val measured = legacy.toMeasuredLength()
        val converted = measured.toLegacyLength()

        converted.inKilometer().shouldBeNear(1.5, 1e-10)
    }

    @Test
    fun `질량 라운드트립 변환이 동작한다`() {
        val legacy = io.bluetape4k.units.Weight(2.25, io.bluetape4k.units.WeightUnit.TON)
        val measured = legacy.toMeasuredMass()
        val converted = measured.toLegacyWeight()

        converted.inTon().shouldBeNear(2.25, 1e-10)
    }

    @Test
    fun `면적과 부피 라운드트립 변환이 동작한다`() {
        val legacyArea = io.bluetape4k.units.Area(42.0, io.bluetape4k.units.AreaUnit.METER_2)
        val legacyVolume = io.bluetape4k.units.Volume(3.5, io.bluetape4k.units.VolumeUnit.LITER)

        legacyArea.toMeasuredArea().toLegacyArea().inMeter2().shouldBeNear(42.0, 1e-10)
        legacyVolume.toMeasuredVolume().toLegacyVolume().inLiter().shouldBeNear(3.5, 1e-10)
    }

    @Test
    fun `각도 압력 저장용량 라운드트립 변환이 동작한다`() {
        val angle = io.bluetape4k.units.Angle(180.0, io.bluetape4k.units.AngleUnit.DEGREE)
        val pressure = io.bluetape4k.units.Pressure(1.0, io.bluetape4k.units.PressureUnit.ATM)
        val storage = io.bluetape4k.units.Storage(2.0, io.bluetape4k.units.StorageUnit.GBYTE)

        angle.toMeasuredAngle().toLegacyAngle().inDegree().shouldBeNear(180.0, 1e-10)
        pressure.toMeasuredPressure().toLegacyPressure().inAtm().shouldBeNear(1.0, 1e-10)
        storage.toMeasuredStorage().toLegacyStorage().inGBytes().shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `절대온도 라운드트립 변환이 동작한다`() {
        val legacy = io.bluetape4k.units.Temperature.celsius(25.0)
        val measured = legacy.toMeasuredTemperature()
        val converted = measured.toLegacyTemperature()

        converted.inCelcius().shouldBeNear(25.0, 1e-10)
    }
}
