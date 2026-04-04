package io.bluetape4k.geocode.google

import com.google.maps.model.AddressComponentType
import com.google.maps.model.GeocodingResult
import com.google.maps.model.LatLng
import io.bluetape4k.geocode.Address
import io.bluetape4k.geocode.Geocode

/**
 * [Geocode]를 Google map의 [LatLng] 로 변환합니다.
 *
 * ## 동작/계약
 * - [scale]이 현재 스케일과 다르면 [Geocode.round] 후 변환합니다.
 * - 원본 [Geocode]를 mutate하지 않습니다.
 *
 * ```kotlin
 * val latLng = Geocode(37.5665, 126.9780).toLatLng(scale = 3)
 * // latLng.lat == 37.567
 * ```
 *
 * @param scale
 * @return [LatLng] 인스턴스
 */
fun Geocode.toLatLng(scale: Int = this.scale): LatLng {
    val geocode = when (scale) {
        this.scale -> this
        else       -> this.round(scale)
    }
    return LatLng(geocode.latitude.toDouble(), geocode.longitude.toDouble())
}

/**
 * 구글 맵의 [GeocodingResult] 을 [Address] 로 변환합니다.
 *
 * ## 동작/계약
 * - country/city/detailAddress/zipCode를 추출해 [GoogleAddress]로 매핑합니다.
 * - 입력 결과 객체를 mutate하지 않습니다.
 *
 * ```kotlin
 * val address = result.toAddress()
 * // address.formattedAddress == result.formattedAddress
 * ```
 *
 * @return [Address] 인스턴스
 */
fun GeocodingResult.toAddress(): GoogleAddress =
    GoogleAddress(
        placeId = this.placeId,
        country = this.country,
        city = this.city,
        detailAddress = this.detailAddress,
        zipCode = this.zipCode,
        formattedAddress = this.formattedAddress
    )


/**
 * 국가명 컴포넌트를 조회합니다.
 *
 * ```kotlin
 * val country = result.country
 * // country == "South Korea"
 * ```
 */
val GeocodingResult.country: String?
    get() = addressComponents.find { it.types.contains(AddressComponentType.COUNTRY) }?.longName

/**
 * 도시(행정구역 레벨1) 이름을 조회합니다.
 *
 * ```kotlin
 * val city = result.city
 * // city == "Seoul"
 * ```
 */
val GeocodingResult.city: String?
    get() = addressComponents.find { it.types.contains(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1) }?.longName

/**
 * 상세 주소 문자열을 조합해 반환합니다.
 *
 * ```kotlin
 * val detail = result.detailAddress
 * // detail.isNotBlank()
 * ```
 */
val GeocodingResult.detailAddress: String
    get() = addressComponents.find { it.types.contains(AddressComponentType.PREMISE) }?.longName + " " +
            addressComponents
                .filter { it.types.contains(AddressComponentType.SUBLOCALITY) }
                .joinToString(" ") { it.longName }

/**
 * 우편번호 컴포넌트를 조회합니다.
 *
 * ```kotlin
 * val zipCode = result.zipCode
 * // zipCode == "06627"
 * ```
 */
val GeocodingResult.zipCode: String?
    get() = addressComponents.find { it.types.contains(AddressComponentType.POSTAL_CODE) }?.longName

/**
 * GeocodingResult
 * placeId=ChIJ-emr_U-hfDURe39Gno-JAf4
 * [Geometry: 37.49205960,127.02978600 (ROOFTOP) bounds=null,
 *   viewport=[37.49340858,127.03113498, 37.49071062,127.02843702]],
 *   formattedAddress=327 Gangnam-daero, Seocho-gu, Seoul, South Korea,
 *   types=[establishment, point_of_interest],
 *
 *   addressComponents=[
 *      [AddressComponent: "327" ("327") (premise)],
 *      [AddressComponent: "Gangnam-daero" ("Gangnam-daero") (political, sublocality, sublocality_level_4)],
 *      [AddressComponent: "Seocho-gu" ("Seocho-gu") (political, sublocality, sublocality_level_1)],
 *      [AddressComponent: "Seoul" ("Seoul") (administrative_area_level_1, political)],
 *      [AddressComponent: "South Korea" ("KR") (country, political)],
 *      [AddressComponent: "06627" ("06627") (postal_code)]]]
 */
