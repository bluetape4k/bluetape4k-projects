package io.bluetape4k

/**
 * [ValueObject]의 최상위 추상화 클래스입니다.
 */
abstract class AbstractValueObject: ValueObject {

    /**
     * Class의 고유성을 표현하는 속성들이 같은지 비교한다 (Business Key)
     */
    protected open fun equalProperties(other: Any): Boolean {
        return other is ValueObject && hashCode() == other.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        return equalProperties(other)
    }

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun toString(): String = buildStringHelper().toString()

    open fun toString(limit: Int): String = buildStringHelper().toString(limit)

    protected open fun buildStringHelper(): ToStringBuilder = ToStringBuilder(this)
}
