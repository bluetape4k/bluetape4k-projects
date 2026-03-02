package io.bluetape4k.geocode

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.ToStringBuilder
import io.bluetape4k.support.hashOf

/**
 * 주소를 나타냅니다. 위경도로 주소정보를 얻어오는 Reverse Geocode 에서 검색 결과에 해당합니다.
 *
 * ## 동작/계약
 * - [country], [city]를 기준으로 값 동등성을 판단합니다.
 * - 하위 클래스는 추가 필드를 가질 수 있으며 문자열 표현은 [buildStringHelper]를 통해 확장됩니다.
 *
 * ```kotlin
 * val a1 = GoogleAddress(country = "Korea", city = "Seoul")
 * val a2 = GoogleAddress(country = "Korea", city = "Seoul")
 * // a1 == a2
 * ```
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
