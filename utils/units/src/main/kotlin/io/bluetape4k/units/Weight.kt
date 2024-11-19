package io.bluetape4k.units

import io.bluetape4k.support.unsafeLazy
import java.io.Serializable
import kotlin.math.absoluteValue
import kotlin.math.sign

fun weightOf(value: Number = 0.0, unit: WeightUnit = WeightUnit.GRAM): Weight = Weight(value, unit)

fun <T: Number> T.weightBy(unit: WeightUnit) = weightOf(this.toDouble(), unit)

fun <T: Number> T.milligram(): Weight = weightBy(WeightUnit.MILLIGRAM)
fun <T: Number> T.gram(): Weight = weightBy(WeightUnit.GRAM)
fun <T: Number> T.kilogram(): Weight = weightBy(WeightUnit.KILOGRAM)
fun <T: Number> T.ton(): Weight = weightBy(WeightUnit.TON)

operator fun <T: Number> T.times(weight: Weight): Weight = weight.times(this)

/**
 * 무게 단위
 *
 * @property unitName  단위 약어
 * @property factor  단위 Factor
 */
enum class WeightUnit(val unitName: String, val factor: Double) {
    MILLIGRAM("mg", 1e-3),
    GRAM("g", 1.0),
    KILOGRAM("kg", 1e3),
    TON("ton", 1e6);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): WeightUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.unitName == lower }
                ?: throw NumberFormatException("Unknown Weight unit. unitStr=$unitStr")
        }
    }
}

/**
 * 무게를 표현하는 클래스
 *
 * ```
 * val weight = Weight(100.0, WeightUnit.GRAM)
 * val weight4 = 100.0.gram()
 * val weight7 = 100.0 * Weight(100.0, WeightUnit.GRAM)
 * val weight10 = 100.0 * 100.0.gram()
 * val weight13 = Weight.parse("100.0 g")
 * weight13.toHuman() // "100.0 g"
 * ```
 *
 * @property value 그램(g) 단위의 값
 */
@JvmInline
value class Weight(val value: Double = 0.0): Comparable<Weight>, Serializable {

    operator fun plus(other: Weight): Weight = Weight(value + other.value)
    operator fun minus(other: Weight): Weight = Weight(value - other.value)
    operator fun times(scalar: Number): Weight = Weight(value * scalar.toDouble())
    operator fun div(scalar: Number): Weight = Weight(value / scalar.toDouble())

    operator fun unaryMinus(): Weight = Weight(-value)

    fun getValueBy(unit: WeightUnit): Double = value / unit.factor

    fun inMilligram(): Double = value / WeightUnit.MILLIGRAM.factor
    fun inGram(): Double = value / WeightUnit.GRAM.factor
    fun inKillogram(): Double = value / WeightUnit.KILOGRAM.factor
    fun inTon(): Double = value / WeightUnit.TON.factor

    fun toHuman(): String {
        var unit = WeightUnit.GRAM
        var display = value.absoluteValue

        if (display > WeightUnit.TON.factor) {
            display /= WeightUnit.TON.factor
            unit = WeightUnit.TON
            return formatUnit(display * value.sign, unit.unitName)
        }
        if (display < WeightUnit.GRAM.factor) {
            unit = WeightUnit.MILLIGRAM
            display /= WeightUnit.MILLIGRAM.factor
        } else if (display > WeightUnit.KILOGRAM.factor) {
            unit = WeightUnit.KILOGRAM
            display /= WeightUnit.KILOGRAM.factor
        }
        return formatUnit(display * value.sign, unit.unitName)
    }

    fun toHuman(unit: WeightUnit): String =
        formatUnit(value / unit.factor, unit.unitName)

    override fun compareTo(other: Weight): Int = value.compareTo(other.value)

    companion object {
        @JvmStatic
        val ZERO: Weight by unsafeLazy { Weight(0.0) }

        @JvmStatic
        val NaN: Weight by unsafeLazy { Weight(Double.NaN) }

        operator fun invoke(value: Number = 0.0, unit: WeightUnit): Weight =
            Weight(value.toDouble() * unit.factor)

        fun parse(expr: String?): Weight {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (value, unit) = expr.trim().split(" ", limit = 2)
                return Weight(value.toDouble(), WeightUnit.parse(unit))
            } catch (e: Exception) {
                throw NumberFormatException("Invalid Weight string. expr=$expr")
            }
        }
    }
}
