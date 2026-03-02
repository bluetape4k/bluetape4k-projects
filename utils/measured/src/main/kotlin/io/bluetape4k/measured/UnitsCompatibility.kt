@file:Suppress("DEPRECATION")

package io.bluetape4k.measured

import io.bluetape4k.units.Angle as LegacyAngle
import io.bluetape4k.units.AngleUnit as LegacyAngleUnit
import io.bluetape4k.units.Area as LegacyArea
import io.bluetape4k.units.AreaUnit as LegacyAreaUnit
import io.bluetape4k.units.Length as LegacyLength
import io.bluetape4k.units.LengthUnit as LegacyLengthUnit
import io.bluetape4k.units.Pressure as LegacyPressure
import io.bluetape4k.units.PressureUnit as LegacyPressureUnit
import io.bluetape4k.units.Storage as LegacyStorage
import io.bluetape4k.units.StorageUnit as LegacyStorageUnit
import io.bluetape4k.units.Temperature as LegacyTemperature
import io.bluetape4k.units.Volume as LegacyVolume
import io.bluetape4k.units.VolumeUnit as LegacyVolumeUnit
import io.bluetape4k.units.Weight as LegacyWeight
import io.bluetape4k.units.WeightUnit as LegacyWeightUnit

/**
 * legacy [LegacyLength]를 measured 길이 측정값으로 변환합니다.
 *
 * ## 동작/계약
 * - legacy 길이를 미터값으로 읽어 measured [Length.meters]로 변환합니다.
 * - 변환은 새 객체를 반환하며 원본을 변경하지 않습니다.
 *
 * ```kotlin
 * val measured = legacyLength.toMeasuredLength()
 * // measured `in` Length.meters == legacyLength.inMeter()
 * ```
 */
fun LegacyLength.toMeasuredLength(): Measure<Length> = inMeter() * Length.meters

/**
 * legacy [LegacyWeight]를 measured 질량 측정값으로 변환합니다.
 */
fun LegacyWeight.toMeasuredMass(): Measure<Mass> = inKilogram() * Mass.kilograms

/**
 * legacy [LegacyArea]를 measured 면적 측정값으로 변환합니다.
 */
fun LegacyArea.toMeasuredArea(): Measure<Area> = inMeter2() * Area.meters2

/**
 * legacy [LegacyVolume]을 measured 부피 측정값으로 변환합니다.
 */
fun LegacyVolume.toMeasuredVolume(): Measure<Volume> = inMeter3() * Volume.cubicMeters

/**
 * legacy [LegacyAngle]을 measured 각도 측정값으로 변환합니다.
 */
fun LegacyAngle.toMeasuredAngle(): Measure<Angle> = inDegree() * Angle.degrees

/**
 * legacy [LegacyPressure]를 measured 압력 측정값으로 변환합니다.
 */
fun LegacyPressure.toMeasuredPressure(): Measure<Pressure> = inPascal() * Pressure.pascal

/**
 * legacy [LegacyStorage]를 measured 저장용량 측정값으로 변환합니다.
 */
fun LegacyStorage.toMeasuredStorage(): Measure<Storage> = inBytes() * Storage.bytes

/**
 * legacy [LegacyTemperature]를 measured 절대온도로 변환합니다.
 */
fun LegacyTemperature.toMeasuredTemperature(): Temperature = inKelvin().kelvin()

/**
 * measured 길이 측정값을 legacy [LegacyLength]로 변환합니다.
 *
 * ## 동작/계약
 * - measured 길이를 미터값으로 변환해 legacy `METER` 단위로 생성합니다.
 * - 역변환 시 수치 오차는 `Double` 연산 정밀도에 따릅니다.
 *
 * ```kotlin
 * val legacy = measuredLength.toLegacyLength()
 * // legacy.inMeter() == (measuredLength `in` Length.meters)
 * ```
 */
fun Measure<Length>.toLegacyLength(): LegacyLength = LegacyLength(this `in` Length.meters, LegacyLengthUnit.METER)

/**
 * measured 질량 측정값을 legacy [LegacyWeight]로 변환합니다.
 */
fun Measure<Mass>.toLegacyWeight(): LegacyWeight = LegacyWeight(this `in` Mass.kilograms, LegacyWeightUnit.KILOGRAM)

/**
 * measured 면적 측정값을 legacy [LegacyArea]로 변환합니다.
 */
fun Measure<Area>.toLegacyArea(): LegacyArea = LegacyArea(this `in` Area.meters2, LegacyAreaUnit.METER_2)

/**
 * measured 부피 측정값을 legacy [LegacyVolume]으로 변환합니다.
 */
fun Measure<Volume>.toLegacyVolume(): LegacyVolume = LegacyVolume(this `in` Volume.cubicMeters, LegacyVolumeUnit.METER_3)

/**
 * measured 각도 측정값을 legacy [LegacyAngle]로 변환합니다.
 */
fun Measure<Angle>.toLegacyAngle(): LegacyAngle = LegacyAngle(this `in` Angle.degrees, LegacyAngleUnit.DEGREE)

/**
 * measured 압력 측정값을 legacy [LegacyPressure]로 변환합니다.
 */
fun Measure<Pressure>.toLegacyPressure(): LegacyPressure = LegacyPressure(this `in` Pressure.pascal, LegacyPressureUnit.PASCAL)

/**
 * measured 저장용량 측정값을 legacy [LegacyStorage]로 변환합니다.
 */
fun Measure<Storage>.toLegacyStorage(): LegacyStorage = LegacyStorage(this `in` Storage.bytes, LegacyStorageUnit.BYTE)

/**
 * measured 절대온도를 legacy [LegacyTemperature]로 변환합니다.
 */
fun Temperature.toLegacyTemperature(): LegacyTemperature = LegacyTemperature.kelvin(inKelvin())
