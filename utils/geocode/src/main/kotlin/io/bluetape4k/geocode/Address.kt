package io.bluetape4k.geocode

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf

/**
 * 주소를 나타냅니다. 위경도로 주소정보를 얻어오는 Reverse Geocode 에서 검색 결과에 해당합니다.
 *
 * @property country 국가 이름
 * @property city 도시 이름
 */
abstract class Address(
    val country: String? = null,
    val city: String? = null,
): AbstractValueObject() {

    override fun equalProperties(other: Any): Boolean =
        other is Address && country == other.country && city == other.city

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = hashOf(country, city)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("country", country)
            .add("city", city)
    }
}
