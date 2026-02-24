package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import kotlin.math.absoluteValue

fun volumeOf(
    volume: Number = 0.0,
    unit: VolumeUnit = VolumeUnit.LITER,
): Volume = Volume(volume, unit)

fun <T: Number> T.volumeBy(unit: VolumeUnit): Volume = Volume(this, unit)

fun <T: Number> T.cc(): Volume = volumeBy(VolumeUnit.CC)

fun <T: Number> T.milliliter(): Volume = volumeBy(VolumeUnit.MILLILITER)

fun <T: Number> T.deciliter(): Volume = volumeBy(VolumeUnit.DECILITER)

fun <T: Number> T.liter(): Volume = volumeBy(VolumeUnit.LITER)

fun <T: Number> T.centimeter3(): Volume = volumeBy(VolumeUnit.CENTIMETER_3)

fun <T: Number> T.meter3(): Volume = volumeBy(VolumeUnit.METER_3)

operator fun <T: Number> T.times(volume: Volume): Volume = volume * this

/**
 * 체적 (Volume) 종류 및 단위
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
enum class VolumeUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {
    CC("cc", 1.0e-9),
    CENTIMETER_3("cm^3", 1.0e-3),
    MILLILITER("ml", 1.0e-3),
    DECILITER("dl", 1.0e-2),
    LITER("l", 1.0),
    METER_3("m^3", 1.0e3),
    ;

    // 영국 부피 단위는 따로 클래스를 만들 예정입니다.
    //    GALLON("gl", 1.0 / 0.264172),
    //    BARREL("barrel", 1.0 / 0.006293),
    //    FLUID_OUNCE("oz", 1.0 / 33.814022);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): VolumeUnit {
            var lower = unitStr.lowercase()
            if (lower.endsWith("s")) lower = lower.dropLast(1)

            return entries.find { it.unitName == lower }
                ?: throw IllegalArgumentException("Unknown Volume unit. unitStr=$unitStr")
        }
    }
}

/**
 * 체적 (Volume) 을 나타내는 클래스. CC를 기본단위로 사용합니다.
 *
 * ```
 * val volume = Volume(100.0, VolumeUnit.LITER)
 * val volume4 = 100.0.liter()
 * val volume7 = 100.0 * Volume(100.0, VolumeUnit.LITER)
 * val volume10 = 100.0 * 100.0.liter()
 * val volume13 = Volume.parse("100.0 l")
 * volume13.toHuman() // "100.0 l"
 * ```
 *
 * @property value CC 단위의 값
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
@JvmInline
value class Volume(
    override val value: Double = 0.0,
): Measurable<VolumeUnit> {
    operator fun plus(other: Volume): Volume = Volume(value + other.value)

    operator fun minus(other: Volume): Volume = Volume(value - other.value)

    operator fun times(scalar: Number): Volume = Volume(value * scalar.toDouble())

    operator fun div(scalar: Number): Volume = Volume(value / scalar.toDouble())

    operator fun div(area: Area): Length = Length(inCC() / area.value)

    operator fun div(length: Length): Area = Area(inCC() / length.value)

    operator fun unaryMinus(): Volume = Volume(-value)

    fun inCC() = valueBy(VolumeUnit.CC)

    fun inMilliLiter() = valueBy(VolumeUnit.MILLILITER)

    fun inDeciLiter() = valueBy(VolumeUnit.DECILITER)

    fun inLiter() = valueBy(VolumeUnit.LITER)

    fun inCentiMeter3() = valueBy(VolumeUnit.CENTIMETER_3)

    fun inMeter3() = valueBy(VolumeUnit.METER_3)

    override fun convertTo(newUnit: VolumeUnit): Volume = Volume(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        val absValue = value.absoluteValue
        val displayUnit = VolumeUnit.entries.lastOrNull { absValue / it.factor > 1.0 } ?: VolumeUnit.CC

        return formatUnit(value / displayUnit.factor, displayUnit.unitName)
    }

    companion object: KLogging() {
        @JvmStatic
        val ZERO: Volume by unsafeLazy { Volume(0.0) }

        @JvmStatic
        val MaxValue: Volume by unsafeLazy { Volume(Double.MAX_VALUE) }

        @JvmStatic
        val MinValue: Volume by unsafeLazy { Volume(Double.MIN_VALUE) }

        @JvmStatic
        val PositiveInf: Volume by unsafeLazy { Volume(Double.POSITIVE_INFINITY) }

        @JvmStatic
        val NegativeInf: Volume by unsafeLazy { Volume(Double.NEGATIVE_INFINITY) }

        @JvmStatic
        val NaN: Volume by unsafeLazy { Volume(Double.NaN) }

        operator fun invoke(
            volume: Number,
            unit: VolumeUnit = VolumeUnit.LITER,
        ): Volume = Volume(volume.toDouble() * unit.factor)

        fun parse(expr: String?): Volume {
            if (expr.isNullOrBlank()) return NaN

            try {
                val (vol, unit) = expr.trim().split(" ", limit = 2)
                return Volume(vol.toDouble(), VolumeUnit.parse(unit))
            } catch (e: Exception) {
                throw IllegalArgumentException("Unknown Volume string. expr=$expr")
            }
        }
    }
}
