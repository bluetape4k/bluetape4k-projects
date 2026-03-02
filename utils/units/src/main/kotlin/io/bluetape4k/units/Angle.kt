package io.bluetape4k.units

import io.bluetape4k.logging.KLogging

/**
 * 각도 값을 생성합니다.
 *
 * ## 동작/계약
 * - 입력 [value]를 [unit] 기준으로 받아 내부 기준 단위(도)로 저장합니다.
 * - 새 [Angle] 인스턴스를 반환하며 입력 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val angle = angleOf(PI, AngleUnit.RADIAN)
 * // angle.inDegree() == 180.0
 * ```
 */
fun angleOf(
    value: Number = 0.0,
    unit: AngleUnit = AngleUnit.DEGREE,
) = Angle(value, unit)

/**
 * 숫자를 지정 단위 각도로 변환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [angleOf]를 호출합니다.
 *
 * ```kotlin
 * val angle = 90.angleBy(AngleUnit.DEGREE)
 * // angle.inRadian() == PI / 2
 * ```
 */
fun <T: Number> T.angleBy(unit: AngleUnit): Angle = angleOf(this.toDouble(), unit)

/**
 * 숫자를 도 단위 각도로 변환합니다.
 *
 * ## 동작/계약
 * - 반환값은 내부적으로 degree 기반 [Angle]입니다.
 *
 * ```kotlin
 * val angle = 180.degree()
 * // angle.inRadian() == PI
 * ```
 */
fun <T: Number> T.degree(): Angle = Angle.degree(this)

/**
 * 숫자를 라디안 단위 각도로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 라디안을 도 기준 내부값으로 변환해 저장합니다.
 *
 * ```kotlin
 * val angle = PI.radian()
 * // angle.inDegree() == 180.0
 * ```
 */
fun <T: Number> T.radian(): Angle = Angle.radian(this)

operator fun <T: Number> T.times(angle: Angle): Angle = angle.times(this)

/**
 * 각도(Angle) 단위
 *
 * ## 동작/계약
 * - [DEGREE]는 기준 단위이며 [RADIAN]은 `180/PI` factor를 사용합니다.
 * - [parse]는 단위 문자열 끝의 복수형 `s`를 제거한 뒤 매칭합니다.
 *
 * ```kotlin
 * val unit = AngleUnit.DEGREE
 * val unit2 = AngleUnit.parse("deg")
 * // unit2 == AngleUnit.DEGREE
 * ```
 *
 * @property unitName 단위 약어
 * @property factor 단위 factor
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
enum class AngleUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {
    /** 도(degree) 단위입니다. */
    DEGREE("deg", 1.0),
    /** 라디안(radian) 단위입니다. */
    RADIAN("rad", 180.0 / Math.PI),
    ;

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
 * ## 동작/계약
 * - 내부 값은 도(degree) 기준으로 저장됩니다.
 * - 덧셈/뺄셈/스칼라 연산은 새 인스턴스를 반환하며 수신 객체를 변경하지 않습니다.
 * - [parse]는 `"<숫자> <단위>"` 형식만 허용하며 실패 시 [IllegalArgumentException]을 던집니다.
 *
 * ```kotlin
 * val angle = Angle(90.0, AngleUnit.DEGREE)
 * val text = angle.toHuman()
 * // text == "90.0 deg"
 * val parsed = Angle.parse("180 deg")
 * // parsed.inRadian() == PI
 * ```
 *
 * @property value  Degree 단위의 각도 값
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
@JvmInline
value class Angle(
    override val value: Double,
): Measurable<AngleUnit> {
    operator fun plus(other: Angle): Angle = Angle(value + other.value)

    operator fun minus(other: Angle): Angle = Angle(value - other.value)

    operator fun times(scalar: Number): Angle = Angle(value * scalar.toDouble())

    operator fun div(scalar: Number): Angle = Angle(value / scalar.toDouble())

    operator fun unaryMinus(): Angle = Angle(-value)

    fun inDegree(): Double = valueBy(AngleUnit.DEGREE)

    fun inRadian(): Double = valueBy(AngleUnit.RADIAN)

    override fun convertTo(newUnit: AngleUnit): Angle = Angle(valueBy(newUnit), newUnit)

    override fun toHuman(): String = formatUnit(value % 360.0, AngleUnit.DEGREE.unitName)

    companion object: KLogging() {
        @JvmStatic
        val ZERO: Angle = Angle(0.0)

        @JvmStatic
        val NaN: Angle = Angle(Double.NaN)

        @JvmStatic
        operator fun invoke(
            value: Number = 0.0,
            unit: AngleUnit = AngleUnit.DEGREE,
        ): Angle = Angle(value.toDouble() * unit.factor)

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
