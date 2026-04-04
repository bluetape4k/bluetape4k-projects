package io.bluetape4k.measured

import kotlin.math.abs
import kotlin.math.round

/**
 * 측정 단위를 나타내는 기본 타입입니다.
 *
 * ## 동작/계약
 * - [ratio]는 기준 단위 대비 배율이며 단위 변환 시 곱셈/나눗셈 기준으로 사용됩니다.
 * - 인스턴스는 불변 값 객체처럼 사용되며 단위 연산은 새 객체를 반환합니다.
 *
 * ```kotlin
 * val meter = Length.meters
 * val km = Length.kilometers
 * // km.ratio == 1000.0
 * // meter < km
 * ```
 */
abstract class Units(
    val suffix: String,
    val ratio: Double = 1.0,
) {
    /**
     * 수치와 단위 사이에 공백을 넣을지 여부입니다.
     */
    protected open val spaceBetweenMagnitude: Boolean = true

    internal fun measureSuffix(): String = if (spaceBetweenMagnitude) " $suffix" else suffix

    override fun toString(): String = suffix

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Units) return false

        return suffix == other.suffix &&
            ratio == other.ratio &&
            spaceBetweenMagnitude == other.spaceBetweenMagnitude
    }

    override fun hashCode(): Int {
        var result = suffix.hashCode()
        result = 31 * result + ratio.hashCode()
        result = 31 * result + spaceBetweenMagnitude.hashCode()
        return result
    }
}

/**
 * 단위의 곱을 나타냅니다.
 *
 * ## 동작/계약
 * - `A * B` 결과 단위로 suffix/ratio를 조합합니다.
 * - 동일 단위 곱인 경우 suffix를 `"(u)^2"` 형태로 만듭니다.
 *
 * ```kotlin
 * val areaUnit = Length.meters * Length.meters
 * // areaUnit.suffix == "(m)^2"
 * ```
 */
class UnitsProduct<A: Units, B: Units>(
    val first: A,
    val second: B,
): Units(
    suffix = if (first == second) "($first)^2" else "$first*$second",
    ratio = first.ratio * second.ratio,
)

/**
 * 단위의 비율(나눗셈)을 나타냅니다.
 *
 * ## 동작/계약
 * - `A / B` 결과 단위를 표현하며 `ratio = A.ratio / B.ratio`입니다.
 * - [reciprocal]은 지연 계산으로 역수 단위를 생성해 캐시합니다.
 *
 * ```kotlin
 * val speedUnit = Length.meters / Time.seconds
 * // speedUnit.suffix == "m/s"
 * // speedUnit.reciprocal.suffix == "s/m"
 * ```
 */
class UnitsRatio<A: Units, B: Units>(
    val numerator: A,
    val denominator: B,
): Units(
    suffix = "$numerator/$denominator",
    ratio = numerator.ratio / denominator.ratio,
) {
    /** 역수 단위입니다. */
    val reciprocal: UnitsRatio<B, A> by lazy { UnitsRatio(denominator, numerator) }
}

/**
 * 역수 단위를 나타냅니다.
 *
 * ## 동작/계약
 * - suffix는 `1/<unit>` 형식입니다.
 * - ratio는 원 단위 ratio의 역수입니다.
 *
 * ```kotlin
 * val inverse = InverseUnits(Time.seconds)
 * // inverse.suffix == "1/s"
 * ```
 */
class InverseUnits<T: Units>(val unit: T): Units("1/${unit.suffix}", 1.0 / unit.ratio)

/**
 * 제곱 단위 별칭입니다.
 */
typealias Square<T> = UnitsProduct<T, T>

/**
 * 측정값(값 + 단위)을 표현합니다.
 *
 * ## 동작/계약
 * - 단위 변환 시 `amount * (from.ratio / to.ratio)` 공식을 사용합니다.
 * - 덧셈/뺄셈은 더 작은 단위로 맞춘 뒤 계산합니다.
 * - 모든 연산은 새 [Measure]를 반환하며 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val sum = 500.meters() + 1.kilometers()
 * // sum `in` Length.meters == 1500.0
 * ```
 */
class Measure<T: Units>(
    val amount: Double,
    val units: T,
): Comparable<Measure<T>> {
    /**
     * 동일 계열 단위로 변환한 측정값을 반환합니다.
     */
    infix fun <A: T> `as`(other: A): Measure<T> = if (units == other) this else Measure(this `in` other, other)

    /**
     * 지정 단위에서의 수치값만 반환합니다.
     */
    infix fun <A: T> `in`(other: A): Double = if (units == other) amount else amount * (units.ratio / other.ratio)

    /**
     * 두 측정값을 더합니다.
     *
     * ## 동작/계약
     * - 더 작은 단위로 맞춘 뒤 수치를 합산합니다.
     *
     * ```kotlin
     * val sum = 500.meters() + 1.kilometers()
     * // sum `in` Length.meters == 1500.0
     * ```
     */
    operator fun plus(other: Measure<T>): Measure<T> {
        val resultUnit = minOf(units, other.units)
        return Measure((this `in` resultUnit) + (other `in` resultUnit), resultUnit)
    }

    /**
     * 두 측정값의 차이를 계산합니다.
     *
     * ## 동작/계약
     * - 더 작은 단위로 맞춘 뒤 수치를 뺍니다.
     *
     * ```kotlin
     * val diff = 2.kilometers() - 500.meters()
     * // diff `in` Length.meters == 1500.0
     * ```
     */
    operator fun minus(other: Measure<T>): Measure<T> {
        val resultUnit = minOf(units, other.units)
        return Measure((this `in` resultUnit) - (other `in` resultUnit), resultUnit)
    }

    /**
     * 측정값의 부호를 반전합니다.
     *
     * ```kotlin
     * val neg = -10.meters()
     * // neg.amount == -10.0
     * ```
     */
    operator fun unaryMinus(): Measure<T> = Measure(-amount, units)

    /**
     * 측정값을 스칼라 수치로 곱합니다.
     *
     * ```kotlin
     * val doubled = 5.meters() * 2
     * // doubled `in` Length.meters == 10.0
     * ```
     */
    operator fun times(other: Number): Measure<T> = amount * other.toDouble() * units

    /**
     * 측정값을 스칼라 수치로 나눕니다.
     *
     * ```kotlin
     * val half = 10.meters() / 2
     * // half `in` Length.meters == 5.0
     * ```
     */
    operator fun div(other: Number): Measure<T> = amount / other.toDouble() * units

    /**
     * 지정 배수 단위로 반올림합니다.
     *
     * ## 동작/계약
     * - `nearest > 0`이어야 하며, 위반 시 [IllegalArgumentException]이 발생합니다.
     * - 단위는 유지하고 수치만 반올림합니다.
     *
     * ```kotlin
     * val rounded = (10.26 * Length.meters).toNearest(0.1)
     * // rounded `in` Length.meters == 10.3
     * ```
     */
    fun toNearest(nearest: Double): Measure<T> {
        require(nearest > 0.0) { "nearest must be > 0. nearest=$nearest" }
        return (round(amount / nearest) * nearest) * units
    }

    /**
     * 지정 단위로 사람이 읽기 쉬운 문자열을 반환합니다.
     *
     * ## 동작/계약
     * - 내부 수치를 지정 단위로 변환 후 문자열로 포맷합니다.
     *
     * ```kotlin
     * val text = 1500.meters().toHuman(Length.kilometers)
     * // text == "1.5 km"
     * ```
     */
    fun toHuman(unit: T): String = formatHuman(this `in` unit, unit)

    /**
     * 현재 단위 기준으로 사람이 읽기 쉬운 문자열을 반환합니다.
     *
     * ## 동작/계약
     * - 길이/질량/시간 등 알려진 단위군은 1 이상이 되는 가장 큰 단위를 자동 선택합니다.
     * - [Angle]은 `0°..360°` 범위로 정규화해 표시합니다.
     *
     * ```kotlin
     * val text = 1500.meters().toHuman()
     * // text == "1.5 km"
     * ```
     */
    fun toHuman(): String {
        @Suppress("UNCHECKED_CAST")
        return when (units) {
            is Length -> toHumanBy(
                listOf(
                    Length.millimeters,
                    Length.centimeters,
                    Length.meters,
                    Length.kilometers,
                ) as List<T>
            )

            is Mass -> toHumanBy(
                listOf(
                    Mass.grams,
                    Mass.kilograms,
                    Mass.tons,
                ) as List<T>
            )

            is Time -> toHumanBy(
                listOf(
                    Time.milliseconds,
                    Time.seconds,
                    Time.minutes,
                    Time.hours,
                ) as List<T>
            )

            is Area -> toHumanBy(
                listOf(
                    Area.millimeters2,
                    Area.centimeters2,
                    Area.meters2,
                    Area.kilometers2,
                ) as List<T>
            )

            is Volume -> toHumanBy(
                listOf(
                    Volume.cubicMillimeters,
                    Volume.cubicCentimeters,
                    Volume.milliliters,
                    Volume.liters,
                    Volume.cubicMeters,
                ) as List<T>
            )

            is Storage -> toHumanBy(
                listOf(
                    Storage.bytes,
                    Storage.kiloBytes,
                    Storage.megaBytes,
                    Storage.gigaBytes,
                    Storage.teraBytes,
                    Storage.petaBytes,
                ) as List<T>
            )

            is BinarySize -> toHumanBy(
                listOf(
                    BinarySize.bits,
                    BinarySize.bytes,
                    BinarySize.kiloBytes,
                    BinarySize.megaBytes,
                    BinarySize.gigaBytes,
                    BinarySize.teraBytes,
                    BinarySize.petaBytes,
                ) as List<T>
            )

            is Frequency -> toHumanBy(
                listOf(
                    Frequency.hertz,
                    Frequency.kiloHertz,
                    Frequency.megaHertz,
                    Frequency.gigaHertz,
                ) as List<T>
            )

            is Energy -> toHumanBy(
                listOf(
                    Energy.joules,
                    Energy.kiloJoules,
                    Energy.megaJoules,
                    Energy.wattHours,
                    Energy.kiloWattHours,
                ) as List<T>
            )

            is Power -> toHumanBy(
                listOf(
                    Power.milliWatts,
                    Power.watts,
                    Power.kiloWatts,
                    Power.megaWatts,
                    Power.gigaWatts,
                ) as List<T>
            )

            is Pressure -> toHumanBy(
                listOf(
                    Pressure.pascal,
                    Pressure.hectoPascal,
                    Pressure.kiloPascal,
                    Pressure.megaPascal,
                    Pressure.gigaPascal,
                    Pressure.bar,
                    Pressure.atmosphere,
                    Pressure.psi,
                ) as List<T>
            )

            is Angle -> {
                val degree = (((this as Measure<Angle>) `in` Angle.degrees) % 360.0 + 360.0) % 360.0
                formatHuman(degree, Angle.degrees)
            }

            else -> formatHuman(amount, units)
        }
    }

    private fun toHumanBy(candidates: List<T>): String {
        if (candidates.isEmpty()) return formatHuman(amount, units)

        val sorted = candidates.sortedBy { it.ratio }
        val best = sorted.lastOrNull { abs(this `in` it) >= 1.0 } ?: sorted.first()
        return toHuman(best)
    }

    override fun compareTo(other: Measure<T>): Int = (this `as` other.units).amount.compareTo(other.amount)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Measure<*>) return false

        @Suppress("UNCHECKED_CAST")
        other as Measure<T>

        val resultUnit = minOf(units, other.units)
        return (this `in` resultUnit) == (other `in` resultUnit)
    }

    override fun hashCode(): Int = (amount * units.ratio).hashCode()

    override fun toString(): String = "$amount${units.measureSuffix()}"
}

/**
 * 두 단위를 비교합니다.
 *
 * ## 동작/계약
 * - [ratio] 기준 오름차순 비교입니다.
 *
 * ```kotlin
 * // Length.meters < Length.kilometers
 * ```
 */
operator fun <A: Units, B: A> A.compareTo(other: B): Int = ratio.compareTo(other.ratio)

/**
 * 더 작은 단위를 반환합니다.
 *
 * ## 동작/계약
 * - [ratio]가 작은 단위를 반환합니다.
 *
 * ```kotlin
 * val result = minOf(Length.meters, Length.kilometers)
 * // result == Length.meters
 * ```
 */
fun <A: Units, B: A> minOf(first: A, second: B): A = if (first < second) first else second

/**
 * 수치와 단위를 곱해 측정값을 생성합니다.
 *
 * ## 동작/계약
 * - [Number]를 `Double`로 변환해 [Measure]를 생성합니다.
 *
 * ```kotlin
 * val distance = 10 * Length.meters
 * // distance.amount == 10.0
 * ```
 */
operator fun <T: Units> Number.times(unit: T): Measure<T> = Measure(this.toDouble(), unit)

/**
 * 단위 간 곱셈으로 복합 단위를 생성합니다.
 *
 * ## 동작/계약
 * - 새 [UnitsProduct]를 반환합니다.
 *
 * ```kotlin
 * val unit = Length.meters * Length.meters
 * // unit.ratio == 1.0
 * ```
 */
operator fun <A: Units, B: Units> A.times(other: B): UnitsProduct<A, B> = UnitsProduct(this, other)

/**
 * 단위 간 나눗셈으로 비율 단위를 생성합니다.
 *
 * ## 동작/계약
 * - 새 [UnitsRatio]를 반환합니다.
 *
 * ```kotlin
 * val unit = Length.kilometers / Time.hours
 * // unit.suffix == "km/hr"
 * ```
 */
operator fun <A: Units, B: Units> A.div(other: B): UnitsRatio<A, B> = UnitsRatio(this, other)

/**
 * 측정값 간 곱셈 연산입니다.
 *
 * ## 동작/계약
 * - 수치 곱셈 후 단위 곱([UnitsProduct])을 적용합니다.
 *
 * ```kotlin
 * val area = 10.meters() * 2.meters()
 * // area `in` (Length.meters * Length.meters) == 20.0
 * ```
 */
operator fun <A: Units, B: Units> Measure<A>.times(other: Measure<B>): Measure<UnitsProduct<A, B>> =
    amount * other.amount * (units * other.units)

/**
 * 측정값 간 나눗셈 연산입니다.
 *
 * ## 동작/계약
 * - 수치 나눗셈 후 단위 비율([UnitsRatio])을 적용합니다.
 *
 * ```kotlin
 * val speed = 10.meters() / 2.seconds()
 * // speed `in` (Length.meters / Time.seconds) == 5.0
 * ```
 */
operator fun <A: Units, B: Units> Measure<A>.div(other: Measure<B>): Measure<UnitsRatio<A, B>> =
    amount / other.amount * (units / other.units)

/**
 * 비율 단위 측정값에 분모 단위를 곱해 분자를 복원합니다.
 *
 * ## 동작/계약
 * - `(A/B) * B -> A` 규칙으로 계산합니다.
 *
 * ```kotlin
 * val speed = 5.metersPerSecond()
 * val distance = speed * 2.seconds()
 * // distance `in` Length.meters == 10.0
 * ```
 */
@JvmName("timesRatioByDenominator")
operator fun <A: Units, B: Units> Measure<UnitsRatio<A, B>>.times(other: Measure<B>): Measure<A> =
    amount * other.amount * units.numerator

/**
 * 곱 단위 측정값을 구성 단위로 나누어 다른 구성 단위를 복원합니다.
 *
 * ## 동작/계약
 * - `(A*B) / A -> B` 규칙으로 계산합니다.
 *
 * ```kotlin
 * val area = 20.meters2()
 * val width = area / 4.meters()
 * // width `in` Length.meters == 5.0
 * ```
 */
@JvmName("divProductByLeft")
operator fun <A: Units, B: Units> Measure<UnitsProduct<A, B>>.div(other: Measure<A>): Measure<B> =
    amount / other.amount * units.second

/**
 * 수치와 단위를 사람이 읽기 쉬운 문자열로 포맷팅합니다.
 */
internal fun formatHuman(value: Double, unit: Units): String {
    val rendered = when {
        value.isNaN() || value.isInfinite() -> value.toString()
        else                                -> {
            val rounded = round(value * 1_000_000_000.0) / 1_000_000_000.0
            rounded.toString()
        }
    }
    return "$rendered${unit.measureSuffix()}"
}
