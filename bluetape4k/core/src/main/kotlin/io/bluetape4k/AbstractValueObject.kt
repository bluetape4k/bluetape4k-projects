package io.bluetape4k

/**
 * [ValueObject]의 최상위 추상화 클래스입니다.
 *
 * 서브클래스는 반드시 [equalProperties]와 [hashCode]를 오버라이드하여
 * 비즈니스 키 기반의 동등성 비교를 구현해야 합니다.
 *
 * ```kotlin
 * class Money(val amount: BigDecimal, val currency: String) : AbstractValueObject() {
 *     override fun equalProperties(other: Any): Boolean =
 *         other is Money && amount == other.amount && currency == other.currency
 *
 *     override fun hashCode(): Int = Objects.hash(amount, currency)
 *
 *     override fun buildStringHelper() = super.buildStringHelper()
 *         .add("amount", amount)
 *         .add("currency", currency)
 * }
 * ```
 */
abstract class AbstractValueObject: ValueObject {

    /**
     * Class의 고유성을 표현하는 속성들(Business Key)이 같은지 비교합니다.
     *
     * 서브클래스는 이 메서드를 반드시 오버라이드하여 비즈니스 키 필드를 직접 비교해야 합니다.
     */
    protected abstract fun equalProperties(other: Any): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other != null && equalProperties(other)
    }

    abstract override fun hashCode(): Int

    override fun toString(): String = buildStringHelper().toString()

    open fun toString(limit: Int): String = buildStringHelper().toString(limit)

    protected open fun buildStringHelper(): ToStringBuilder = ToStringBuilder(this)
}
