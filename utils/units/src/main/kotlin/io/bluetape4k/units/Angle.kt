package io.bluetape4k.units

import io.bluetape4k.logging.KLogging

fun angleOf(value: Number = 0.0, unit: AngleUnit = AngleUnit.DEGREE) = Angle(value, unit)

fun <T: Number> T.angleBy(unit: AngleUnit): Angle = angleOf(this.toDouble(), unit)

fun <T: Number> T.degree(): Angle = Angle.degree(this)
fun <T: Number> T.radian(): Angle = Angle.radian(this)

operator fun <T: Number> T.times(angle: Angle): Angle = angle.times(this)

/**
 * 각도(Angle) 단위
 *
 * ```
 * val unit = AngleUnit.DEGREE
 * val unit2 = AngleUnit.parse("deg")
 * ```
 *
 * @property unitName 단위 약어
 * @property factor 단위 factor
 */
enum class AngleUnit(override val unitName: String, override val factor: Double): MeasurableUnit {

    DEGREE("deg", 1.0),
    RADIAN("rad", 180.0 / Math.PI);

    companion object {
        @JvmStatic
        fun parse(unitStr: String): AngleUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.unitName == lower }
                ?: throw IllegalArgumentException("Unknown Angle unit. unitStr=$unitStr")
        }
    }
}

/**
 * 각도(Angle)를 나타내는 클래스
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
 * @property value  Degree 단위의 길이 값
 */
@JvmInline
value class Angle(override val value: Double): Measurable<AngleUnit> {

    operator fun plus(other: Angle): Angle = Angle(value + other.value)
    operator fun minus(other: Angle): Angle = Angle(value - other.value)
    operator fun times(scalar: Number): Angle = Angle(value * scalar.toDouble())
    operator fun div(scalar: Number): Angle = Angle(value / scalar.toDouble())
    operator fun unaryMinus(): Angle = Angle(-value)

    fun inDegree(): Double = valueBy(AngleUnit.DEGREE)
    fun inRadian(): Double = valueBy(AngleUnit.RADIAN)

    override fun convertTo(newUnit: AngleUnit): Angle =
        Angle(valueBy(newUnit), newUnit)

    override fun toHuman(): String {
        return formatUnit(value % 360.0, AngleUnit.DEGREE.unitName)
    }

    companion object: KLogging() {
        @JvmStatic
        val ZERO: Angle = Angle(0.0)

        @JvmStatic
        val NaN: Angle = Angle(Double.NaN)

        @JvmStatic
        operator fun invoke(value: Number = 0.0, unit: AngleUnit = AngleUnit.DEGREE): Angle =
            Angle(value.toDouble() * unit.factor)

        fun degree(value: Number): Angle = Angle(value.toDouble(), AngleUnit.DEGREE)
        fun radian(value: Number): Angle = Angle(value.toDouble(), AngleUnit.RADIAN)

        fun parse(expr: String?): Angle {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (valueStr, unitStr) = expr.split(" ", limit = 2)
                return Angle(valueStr.toDouble(), AngleUnit.parse(unitStr))
            } catch (e: Throwable) {
                throw IllegalArgumentException("Invalid Angle expression. expr=$expr", e)
            }
        }
    }
}
