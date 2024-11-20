package io.bluetape4k.geocode.google

import com.google.maps.model.AddressComponentType
import com.google.maps.model.GeocodingResult
import com.google.maps.model.LatLng
import io.bluetape4k.geocode.Address
import io.bluetape4k.geocode.Geocode

/**
 * [Geocode]를 Google map의 [LatLng] 로 변환합니다.
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
 * Country
 */
val GeocodingResult.country: String?
    get() = addressComponents.find { it.types.contains(AddressComponentType.COUNTRY) }?.longName

/**
 * City 정보
 */
val GeocodingResult.city: String?
    get() = addressComponents.find { it.types.contains(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1) }?.longName

/**
 * City 하위의 상세 주소
 */
val GeocodingResult.detailAddress: String
    get() = addressComponents.find { it.types.contains(AddressComponentType.PREMISE) }?.longName + " " +
            addressComponents
                .filter { it.types.contains(AddressComponentType.SUBLOCALITY) }
                .joinToString(" ") { it.longName }

/**
 * 우편번호
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
