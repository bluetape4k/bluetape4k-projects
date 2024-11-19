package io.bluetape4k.units

import io.bluetape4k.support.unsafeLazy
import java.io.Serializable
import kotlin.math.absoluteValue

fun lengthOf(value: Number = 0.0, unit: LengthUnit = LengthUnit.MILLIMETER): Length = Length(value, unit)

fun <T: Number> T.lengthBy(unit: LengthUnit): Length = lengthOf(this.toDouble(), unit)

fun <T: Number> T.millimeter(): Length = lengthBy(LengthUnit.MILLIMETER)
fun <T: Number> T.centimeter(): Length = lengthBy(LengthUnit.CENTIMETER)
fun <T: Number> T.meter(): Length = lengthBy(LengthUnit.METER)
fun <T: Number> T.kilometer(): Length = lengthBy(LengthUnit.KILOMETER)

operator fun <T: Number> T.times(length: Length): Length = length.times(this)

/**
 * 길이 단위
 *
 * ```
 * val unit = LengthUnit.METER
 * val unit2 = LengthUnit.parse("cm")
 * ```
 *
 * @property abbrName 단위 약어
 * @property factor 단위 factor
 * @constructor
 */
enum class LengthUnit(val abbrName: String, val factor: Double) {
    MILLIMETER("mm", 1.0e-3),
    CENTIMETER("cm", 1.0e-1),
    METER("m", 1.0),
    KILOMETER("km", 1.0e3);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): LengthUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.abbrName == lower }
                ?: throw NumberFormatException("Unknown Length unit. unitStr=$unitStr")
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
value class Length(val value: Double = 0.0): Comparable<Length>, Serializable {

    operator fun plus(other: Length): Length = Length(value + other.value)
    operator fun minus(other: Length): Length = Length(value - other.value)
    operator fun times(scalar: Number): Length = Length(value * scalar.toDouble())
    operator fun div(scalar: Number): Length = Length(value / scalar.toDouble())

    operator fun unaryMinus(): Length = Length(-value)

    fun getValueBy(unit: LengthUnit): Double = value / unit.factor

    fun inMillimeter(): Double = getValueBy(LengthUnit.MILLIMETER)
    fun inCentimeter(): Double = getValueBy(LengthUnit.CENTIMETER)
    fun inMeter(): Double = getValueBy(LengthUnit.METER)
    fun inKilometer(): Double = getValueBy(LengthUnit.KILOMETER)

    override fun compareTo(other: Length): Int = value.compareTo(other.value)

    fun toHuman(): String {
        val display = value.absoluteValue
        val displayUnit = LengthUnit.entries.lastOrNull { display / it.factor > 1.0 } ?: LengthUnit.MILLIMETER
        return formatUnit(value / displayUnit.factor, displayUnit.abbrName)
    }

    fun toUnit(unit: LengthUnit): String =
        formatUnit(value / unit.factor, unit.abbrName)

    companion object {
        @JvmStatic
        val ZERO: Length by unsafeLazy { Length(0.0) }

        @JvmStatic
        val NaN: Length by unsafeLazy { Length(Double.NaN) }

        operator fun invoke(value: Number = 0.0, unit: LengthUnit = LengthUnit.MILLIMETER): Length =
            Length(value.toDouble() * unit.factor)

        fun parse(expr: String?): Length {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (valueStr, unitStr) = expr.split(" ", limit = 2)
                return Length(valueStr.toDouble(), LengthUnit.parse(unitStr))
            } catch (e: Exception) {
                throw NumberFormatException("Invalid Length expression. expr=$expr")
            }
        }
    }
}
