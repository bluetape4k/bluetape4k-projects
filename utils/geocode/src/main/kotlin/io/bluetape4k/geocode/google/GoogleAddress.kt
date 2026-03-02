package io.bluetape4k.geocode.google

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.geocode.Address

/**
 * Google Maps API를 통해 찾은 주소 정보입니다.
 *
 * ## 동작/계약
 * - [Address]를 상속하며 country/city 동등성 규칙을 그대로 따릅니다.
 * - Google API 응답의 부가 필드(placeId/zipCode 등)를 추가 보관합니다.
 *
 * ```kotlin
 * val address = GoogleAddress(placeId = "p1", country = "Korea", city = "Seoul")
 * // address.placeId == "p1"
 * ```
 *
 * @property placeId 장소 ID
 * @property country 국가 이름
 * @property city 도시 이름
 * @property detailAddress 상세 주소
 * @property zipCode 우편번호
 * @property formattedAddress 포맷된 주소
 */
class GoogleAddress(
    val placeId: String? = null,
    country: String? = null,
    city: String? = null,
    val detailAddress: String? = null,
    val zipCode: String? = null,
    val formattedAddress: String? = null,
): Address(country, city) {

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("placeId", placeId)
            .add("detailAddress", detailAddress)
            .add("zipCode", zipCode)
            .add("formattedAddress", formattedAddress)
    }
}
