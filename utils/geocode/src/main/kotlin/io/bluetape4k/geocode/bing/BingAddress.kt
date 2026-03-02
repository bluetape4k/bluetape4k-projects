package io.bluetape4k.geocode.bing

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.geocode.Address

/**
 * Bing Maps API를 통해 찾은 주소 정보입니다.
 *
 * ## 동작/계약
 * - [Address]를 상속하며 country/city 동등성 규칙을 공유합니다.
 * - Bing 응답의 추가 필드(name/detail/zip/formatted)를 함께 보관합니다.
 *
 * ```kotlin
 * val address = BingAddress(name = "Gangnam", country = "Korea", city = "Seoul")
 * // address.name == "Gangnam"
 * ```
 *
 * @property name 주소 이름
 * @property country 국가 이름
 * @property city 도시 이름
 * @property detailAddress 상세 주소
 * @property zipCode 우편번호
 * @property formattedAddress 포맷된 주소
 */
class BingAddress(
    val name: String? = null,
    country: String? = null,
    city: String? = null,
    val detailAddress: String? = null,
    val zipCode: String? = null,
    val formattedAddress: String? = null,
): Address(country, city) {

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("detailAddress", detailAddress)
            .add("zipCode", zipCode)
            .add("name", name)
            .add("formattedAddress", formattedAddress)
    }
}
