package io.bluetape4k.units

import io.bluetape4k.support.unsafeLazy
import io.bluetape4k.units.AreaUnit.CENTI_METER_2
import io.bluetape4k.units.AreaUnit.METER_2
import io.bluetape4k.units.AreaUnit.MILLI_METER_2
import java.io.Serializable
import kotlin.math.absoluteValue

fun areaOf(value: Number = 0.0, unit: AreaUnit = MILLI_METER_2) = Area(value, unit)

fun <T: Number> T.areaBy(unit: AreaUnit): Area = Area(this, unit)

fun <T: Number> T.millimeter2(): Area = areaBy(MILLI_METER_2)
fun <T: Number> T.centimeter2(): Area = areaBy(CENTI_METER_2)
fun <T: Number> T.meter2(): Area = areaBy(METER_2)

operator fun <T: Number> T.times(area: Area): Area = area * this

/**
 * 면적을 나타내는 단위를 표현합니다
 */
enum class AreaUnit(val unitName: String, val factor: Double) {

    MILLI_METER_2("mm^2", 1.0e-6),
    CENTI_METER_2("cm^2", 1.0e-4),
    METER_2("m^2", 1.0);
    /** 아르 (Ares, a) (1 a = 100 value) (deprecated) */
    //    ARES("ares", 100.0),
    /** 헥타르(Hectare, ha) (1 ha = 100 ares = 10,000 value) */
    //    HECTARE("hec", 1.0e4),
    //    INCH_2("in^2", INCH_IN_METER * INCH_IN_METER),
    //    FEET_2("ft^2", FEET_IN_METER * FEET_IN_METER),
    //    YARD_2("yd^2", YARD_IN_METER * YARD_IN_METER),
    /** 에이커 (1 ac = 4,046.8564224 value = 0.40468564224 Hx ) */
    // ACRE("ac", 4046.8564224);

    companion object {
        @JvmStatic
        fun parse(unitName: String): AreaUnit {
            var lower = unitName.lowercase()
            if (lower.endsWith("s"))
                lower = lower.dropLast(1)

            return entries.find { it.unitName == lower }
                ?: throw NumberFormatException("Unknown Area unit. unitName=$unitName")
        }
    }
}

/**
 * 면적을 나타내는 클래스
 *
 * ```
 * val area = Area(100.0, AreaUnit.METER_2)
 * val area4 = 100.0.meter2()
 * val area7 = 100.0 * Area(100.0, AreaUnit.METER_2)
 * val area10 = 100.0 * 100.0.meter2()
 * val area13 = Area.parse("100.0 m^2")
 * val area16 = 100.0.meter2().toHuman()  // "100.0 m^2"
 * ```
 *
 * @property value millimeter^2 단위의 면적 값
 */
@JvmInline
value class Area(val value: Double = 0.0): Comparable<Area>, Serializable {

    operator fun plus(other: Area): Area = Area(value + other.value)
    operator fun minus(other: Area): Area = Area(value - other.value)

    operator fun times(length: Length): Volume = Volume(value * length.inMeter(), VolumeUnit.METER_3)
    operator fun times(scalar: Number): Area = Area(value * scalar.toDouble())

    operator fun div(length: Length): Length = Length(value / length.inMeter(), LengthUnit.METER)
    operator fun div(scalar: Number): Area = Area(value / scalar.toDouble())

    operator fun unaryMinus(): Area = Area(-value)

    fun inMillimeter2(): Double = value / MILLI_METER_2.factor
    fun inCentimeter2(): Double = value / CENTI_METER_2.factor
    fun inMeter2(): Double = value

    override fun compareTo(other: Area): Int = value.compareTo(other.value)
    override fun toString() = toHuman(METER_2)

    fun toHuman(): String {
        val absValue = value.absoluteValue
        val displayUnit = AreaUnit.entries.lastOrNull { absValue / it.factor > 1.0 } ?: MILLI_METER_2
        return formatUnit(value / displayUnit.factor, displayUnit.unitName)
    }

    fun toHuman(unit: AreaUnit): String {
        return formatUnit(value / unit.factor, unit.unitName)
    }

    companion object {
        @JvmStatic
        val Zero: Area by unsafeLazy { Area(0.0) }

        @JvmStatic
        val MaxValue: Area by unsafeLazy { Area(Double.MAX_VALUE) }

        @JvmStatic
        val MinValue: Area by unsafeLazy { Area(Double.MIN_VALUE) }

        @JvmStatic
        val PositiveInf: Area by unsafeLazy { Area(Double.POSITIVE_INFINITY) }

        @JvmStatic
        val NegateInf: Area by unsafeLazy { Area(Double.NEGATIVE_INFINITY) }

        @JvmStatic
        val NaN: Area by unsafeLazy { Area(Double.NaN) }

        operator fun invoke(area: Number = 0.0, unit: AreaUnit = MILLI_METER_2): Area =
            Area(area.toDouble() * unit.factor)

        @JvmStatic
        fun parse(expr: String?): Area {
            if (expr.isNullOrBlank())
                return NaN

            try {
                val (v, u) = expr.trim().split(" ", limit = 2)
                return Area(v.toDouble(), AreaUnit.parse(u))

            } catch (e: Exception) {
                throw NumberFormatException("Invalid Area string. expr=$expr")
            }
        }
    }
}
