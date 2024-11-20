package io.bluetape4k.geoip2

import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import com.maxmind.geoip2.record.Traits
import java.io.Serializable
import java.net.InetAddress

/**
 * IP Address 정보로부터 행정 주소 정보를 나타냅니다.
 *
 * @property ipAddress IP Address
 * @property city 도시 이름
 * @property country 국가 이름
 * @property continent 대륙 이름
 * @property geoLocation 위도, 경도 정보
 * @property countryIsoCode 국가 코드
 */
data class Address(
    val ipAddress: String? = null,
    val city: String? = null,
    val country: String? = null,
    val continent: String? = null,
    val geoLocation: GeoLocation? = null,
    val countryIsoCode: String? = null,
): Serializable {

    internal var traits: Traits? = null

    companion object {

        /**
         * City 정보를 기반으로 Address 객체를 생성합니다.
         *
         * @param ipAddress IP Address
         * @param cityResponse City 정보
         * @return Address 객체
         */
        @JvmStatic
        fun fromCity(ipAddress: InetAddress, cityResponse: CityResponse): Address {
            return Address(
                ipAddress = ipAddress.toString(),
                city = cityResponse.city.name,
                country = cityResponse.country.name,
                continent = cityResponse.continent.name,
                geoLocation = GeoLocation.fromLocation(cityResponse.location),
                countryIsoCode = cityResponse.country.isoCode,
            ).apply {
                traits = cityResponse.traits
            }
        }

        /**
         * Country 정보를 기반으로 Address 객체를 생성합니다.
         *
         * @param ipAddress IP Address
         * @param countryResponse Country 정보
         * @return Address 객체
         */
        @JvmStatic
        fun fromCountry(ipAddress: InetAddress, countryResponse: CountryResponse): Address {
            return Address(
                ipAddress = ipAddress.toString(),
                country = countryResponse.country.name,
                continent = countryResponse.continent.name,
                countryIsoCode = countryResponse.country.isoCode,
            ).apply {
                traits = countryResponse.traits
            }
        }
    }
}
