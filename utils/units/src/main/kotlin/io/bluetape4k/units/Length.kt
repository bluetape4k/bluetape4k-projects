package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import kotlin.math.absoluteValue

fun lengthOf(value: Number = 0.0, unit: LengthUnit = LengthUnit.MILLIMETER): Length = Length(value, unit)

fun <T: Number> T.lengthBy(unit: LengthUnit): Length = lengthOf(this.toDouble(), unit)

fun <T: Number> T.millimeter(): Length = lengthOf(this, LengthUnit.MILLIMETER)
fun <T: Number> T.centimeter(): Length = lengthOf(this, LengthUnit.CENTIMETER)
fun <T: Number> T.meter(): Length = lengthOf(this, LengthUnit.METER)
fun <T: Number> T.kilometer(): Length = lengthOf(this, LengthUnit.KILOMETER)

operator fun <T: Number> T.times(length: Length): Length = length.times(this)

/**
 * 길이 단위
 *
 * ```
 * val unit = LengthUnit.METER
 * val unit2 = LengthUnit.parse("cm")
 * ```
 *
 * @property unitName 단위 약어
 * @property factor 단위 factor
 * @constructor
 */
enum class LengthUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {
    MILLIMETER("mm", 1.0),
    CENTIMETER("cm", 10.0),
    METER("m", 1.0e3),
    KILOMETER("km", 1.0e6);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): LengthUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.unitName == lower }
                ?: throw IllegalArgumentException("Unknown Length unit. unitStr=$unitStr")
        }
    }
}

/**
 * 길이를 나타내는 클래스
 *
 * ```
 * val length = Length(100.0, LengthUnit.METER)
 * val length4 = 100.0.meter()
 * val length7 = 100.0 * Length(100.0, LengthUnit.METER)
 * val length10 = 100.0 * 100.0.meter()
 * val length13 = Length.parse("100.0 m")
 * length13.toHuman() // "100.0 m"
 *
 * val length16 = length10 / 100.0  // 100.0.meter()
 * ```
 *
 * @property value  Millimeter 단위의 길이 값
 */
@JvmInline
value class Length(override val value: Double = 0.0): Measurable<LengthUnit> {

    operator fun plus(other: Length): Length = Length(value + other.value)
    operator fun minus(other: Length): Length = Length(value - other.value)
    operator fun times(scalar: Number): Length = Length(value * scalar.toDouble())
    operator fun div(scalar: Number): Length = Length(value / scalar.toDouble())

    operator fun unaryMinus(): Length = Length(-value)

    fun inMillimeter(): Double = valueBy(LengthUnit.MILLIMETER)
    fun inCentimeter(): Double = valueBy(LengthUnit.CENTIMETER)
    fun inMeter(): Double = valueBy(LengthUnit.METER)
    fun inKilometer(): Double = valueBy(LengthUnit.KILOMETER)

    override fun convertTo(newUnit: LengthUnit): Length =
        Length(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        val display = value.absoluteValue
        val displayUnit = LengthUnit.entries.lastOrNull { display / it.factor > 1.0 } ?: LengthUnit.MILLIMETER
        return formatUnit(value / displayUnit.factor, displayUnit.unitName)
    }

    companion object: KLogging() {
        @JvmStatic
        val ZERO: Length by unsafeLazy { Length(0.0) }

        @JvmStatic
        val NaN: Length by unsafeLazy { Length(Double.NaN) }

        @JvmStatic
        operator fun invoke(value: Number = 0.0, unit: LengthUnit = LengthUnit.MILLIMETER): Length =
            Length(value.toDouble() * unit.factor)

        fun parse(expr: String?): Length {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (valueStr, unitStr) = expr.split(" ", limit = 2)
                return Length(valueStr.toDouble(), LengthUnit.parse(unitStr))
            } catch (e: Throwable) {
                throw IllegalArgumentException("Invalid Length expression. expr=$expr")
            }
        }
    }
}
