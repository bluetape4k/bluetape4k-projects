package io.bluetape4k.units

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import io.bluetape4k.units.Weight.Companion.NaN
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * 지정된 값과 단위로 [Weight] 객체를 생성합니다.
 *
 * @param value 무게 값
 * @param unit 무게 단위 (기본값: 그램)
 * @return [Weight] 객체
 */
fun weightOf(
    value: Number = 0.0,
    unit: WeightUnit = WeightUnit.GRAM,
): Weight = Weight(value, unit)

/**
 * Number를 지정된 단위의 [Weight]로 변환합니다.
 *
 * @param unit 무게 단위
 * @return [Weight] 객체
 */
fun <T: Number> T.weightBy(unit: WeightUnit) = weightOf(this.toDouble(), unit)

/**
 * Number를 밀리그램 단위의 [Weight]로 변환합니다.
 */
fun <T: Number> T.milligram(): Weight = weightBy(WeightUnit.MILLIGRAM)

/**
 * Number를 그램 단위의 [Weight]로 변환합니다.
 */
fun <T: Number> T.gram(): Weight = weightBy(WeightUnit.GRAM)

/**
 * Number를 킬로그램 단위의 [Weight]로 변환합니다.
 */
fun <T: Number> T.kilogram(): Weight = weightBy(WeightUnit.KILOGRAM)

/**
 * Number를 톤 단위의 [Weight]로 변환합니다.
 */
fun <T: Number> T.ton(): Weight = weightBy(WeightUnit.TON)

/**
 * Number와 [Weight]의 곱셈 연산을 지원합니다.
 */
operator fun <T: Number> T.times(weight: Weight): Weight = weight.times(this)

/**
 * 무게 단위
 *
 * @property unitName  단위 약어
 * @property factor  단위 Factor
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
enum class WeightUnit(
    override val unitName: String,
    override val factor: Double,
): MeasurableUnit {
    MILLIGRAM("mg", 1e-3),
    GRAM("g", 1.0),
    KILOGRAM("kg", 1e3),
    TON("ton", 1e6),
    ;

    companion object {
        @JvmStatic
        fun parse(unitStr: String): WeightUnit {
            val lower = unitStr.lowercase().dropLastWhile { it == 's' }
            return entries.find { it.unitName == lower }
                ?: throw IllegalArgumentException("Unknown Weight unit. unitStr=$unitStr")
        }
    }
}

/**
 * 무게를 표현하는 클래스
 *
 * 이 클래스는 무게를 그램(g)을 기본 단위로 저장하며, 다양한 무게 단위(밀리그램, 킬로그램, 톤) 간의
 * 변환과 연산을 지원합니다.
 *
 * ```
 * val weight = Weight(100.0, WeightUnit.GRAM)
 * val weight2 = 100.0.gram()
 * val weight3 = 100.0 * Weight(100.0, WeightUnit.GRAM)
 * val weight4 = 100.0 * 100.0.gram()
 * val weight5 = Weight.parse("100.0 g")
 * weight5.toHuman() // "100.0 g"
 *
 * // 단위 변환
 * val kg = 1000.0.gram().inKilogram()  // 1.0
 * val ton = 2000.0.kilogram().inTon()  // 2.0
 *
 * // 연산
 * val total = 50.kilogram() + 30.kilogram()  // 80kg
 * val doubled = 25.kilogram() * 2            // 50kg
 * ```
 *
 * @property value 그램(g) 단위의 값
 */
@Deprecated("bluetape4k-units는 점진적으로 폐기 예정입니다. io.bluetape4k.measured 사용을 권장합니다.")
@JvmInline
value class Weight(
    override val value: Double = 0.0,
): Measurable<WeightUnit> {
    /**
     * 두 무게를 더합니다.
     * @param other 더할 무게
     * @return 더한 결과 [Weight]
     */
    operator fun plus(other: Weight): Weight = Weight(value + other.value)

    /**
     * 두 무게를 뺍니다.
     * @param other 뺄 무게
     * @return 뺀 결과 [Weight]
     */
    operator fun minus(other: Weight): Weight = Weight(value - other.value)

    /**
     * 스칼라 값을 곱합니다.
     * @param scalar 곱할 스칼라 값
     * @return 곱한 결과 [Weight]
     */
    operator fun times(scalar: Number): Weight = Weight(value * scalar.toDouble())

    /**
     * 스칼라 값으로 나눕니다.
     * @param scalar 나눌 스칼라 값
     * @return 나눈 결과 [Weight]
     */
    operator fun div(scalar: Number): Weight = Weight(value / scalar.toDouble())

    /**
     * 부호를 반전합니다.
     * @return 부호가 반전된 [Weight]
     */
    operator fun unaryMinus(): Weight = Weight(-value)

    /**
     * 밀리그램 단위의 값을 반환합니다.
     * @return 밀리그램 단위 값
     */
    fun inMilligram(): Double = valueBy(WeightUnit.MILLIGRAM)

    /**
     * 그램 단위의 값을 반환합니다.
     * @return 그램 단위 값
     */
    fun inGram(): Double = valueBy(WeightUnit.GRAM)

    /**
     * 킬로그램 단위의 값을 반환합니다.
     * @return 킬로그램 단위 값
     */
    fun inKilogram(): Double = valueBy(WeightUnit.KILOGRAM)

    /**
     * 톤 단위의 값을 반환합니다.
     * @return 톤 단위 값
     */
    fun inTon(): Double = valueBy(WeightUnit.TON)

    /**
     * 지정된 단위로 변환합니다.
     * @param newUnit 변환할 대상 단위
     * @return 변환된 [Weight]
     */

    /**
     * 지정된 단위로 변환합니다.
     * @param newUnit 변환할 대상 단위
     * @return 변환된 [Weight]
     */
    override fun convertTo(newUnit: WeightUnit): Weight = Weight(valueBy(newUnit), newUnit)

    /**
     * 사람이 읽기 쉬운 형식으로 변환합니다.
     * 자동으로 적절한 단위(mg, g, kg, ton)를 선택하여 표시합니다.
     *
     * @return 예: "100.0 g", "2.5 kg", "1.5 ton"
     */
    override fun toHuman(): String {
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

    companion object: KLogging() {
        /**
         * 0 무게를 나타내는 상수
         */
        @JvmStatic
        val ZERO: Weight by unsafeLazy { Weight(0.0) }

        /**
         * NaN(Not a Number) 무게를 나타내는 상수
         */
        @JvmStatic
        val NaN: Weight by unsafeLazy { Weight(Double.NaN) }

        /**
         * 지정된 값과 단위로 [Weight]를 생성합니다.
         *
         * @param value 무게 값
         * @param unit 무게 단위
         * @return [Weight] 객체
         */
        @JvmStatic
        operator fun invoke(
            value: Number = 0.0,
            unit: WeightUnit,
        ): Weight = Weight(value.toDouble() * unit.factor)

        /**
         * 문자열을 파싱하여 [Weight] 객체를 생성합니다.
         *
         * @param expr 파싱할 문자열 (예: "100.0 kg", "50.0 g")
         * @return 파싱된 [Weight] 객체. 빈 문자열이나 null인 경우 [NaN] 반환
         * @throws IllegalArgumentException 파싱할 수 없는 형식인 경우
         */
        fun parse(expr: String?): Weight {
            if (expr.isNullOrBlank()) {
                return NaN
            }
            try {
                val (value, unit) = expr.trim().split(" ", limit = 2)
                return Weight(value.toDouble(), WeightUnit.parse(unit))
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid Weight string. expr=$expr")
            }
        }
    }
}
