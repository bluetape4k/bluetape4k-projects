package io.bluetape4k.measured

import kotlin.jvm.JvmName
import kotlin.math.round

/**
 * 측정 단위를 나타내는 기본 타입입니다.
 *
 * @property suffix 사람이 읽기 쉬운 단위 접미사
 * @property ratio 기준 단위 대비 배율
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
 */
class InverseUnits<T: Units>(val unit: T): Units("1/${unit.suffix}", 1.0 / unit.ratio)

/**
 * 제곱 단위 별칭입니다.
 */
typealias Square<T> = UnitsProduct<T, T>

/**
 * 측정값(값 + 단위)을 표현합니다.
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

    operator fun plus(other: Measure<T>): Measure<T> {
        val resultUnit = minOf(units, other.units)
        return Measure((this `in` resultUnit) + (other `in` resultUnit), resultUnit)
    }

    operator fun minus(other: Measure<T>): Measure<T> {
        val resultUnit = minOf(units, other.units)
        return Measure((this `in` resultUnit) - (other `in` resultUnit), resultUnit)
    }

    operator fun unaryMinus(): Measure<T> = Measure(-amount, units)

    operator fun times(other: Number): Measure<T> = amount * other.toDouble() * units

    operator fun div(other: Number): Measure<T> = amount / other.toDouble() * units

    /**
     * 지정 배수 단위로 반올림합니다.
     */
    fun toNearest(nearest: Double): Measure<T> {
        require(nearest > 0.0) { "nearest must be > 0. nearest=$nearest" }
        return (round(amount / nearest) * nearest) * units
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
 */
operator fun <A: Units, B: A> A.compareTo(other: B): Int = ratio.compareTo(other.ratio)

/**
 * 더 작은 단위를 반환합니다.
 */
fun <A: Units, B: A> minOf(first: A, second: B): A = if (first < second) first else second

/**
 * 수치와 단위를 곱해 측정값을 생성합니다.
 */
operator fun <T: Units> Number.times(unit: T): Measure<T> = Measure(this.toDouble(), unit)

/**
 * 단위 간 곱셈으로 복합 단위를 생성합니다.
 */
operator fun <A: Units, B: Units> A.times(other: B): UnitsProduct<A, B> = UnitsProduct(this, other)

/**
 * 단위 간 나눗셈으로 비율 단위를 생성합니다.
 */
operator fun <A: Units, B: Units> A.div(other: B): UnitsRatio<A, B> = UnitsRatio(this, other)

/**
 * 측정값 간 곱셈 연산입니다.
 */
operator fun <A: Units, B: Units> Measure<A>.times(other: Measure<B>): Measure<UnitsProduct<A, B>> =
    amount * other.amount * (units * other.units)

/**
 * 측정값 간 나눗셈 연산입니다.
 */
operator fun <A: Units, B: Units> Measure<A>.div(other: Measure<B>): Measure<UnitsRatio<A, B>> =
    amount / other.amount * (units / other.units)

/**
 * 비율 단위 측정값에 분모 단위를 곱해 분자를 복원합니다.
 */
@JvmName("timesRatioByDenominator")
operator fun <A: Units, B: Units> Measure<UnitsRatio<A, B>>.times(other: Measure<B>): Measure<A> =
    amount * other.amount * units.numerator

/**
 * 곱 단위 측정값을 구성 단위로 나누어 다른 구성 단위를 복원합니다.
 */
@JvmName("divProductByLeft")
operator fun <A: Units, B: Units> Measure<UnitsProduct<A, B>>.div(other: Measure<A>): Measure<B> =
    amount / other.amount * units.second
