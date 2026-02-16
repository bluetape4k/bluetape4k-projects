package io.bluetape4k.geoip2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import java.net.InetAddress

/**
 * IP 주소로부터 City 정보를 조회합니다.
 *
 * ```
 * val cityResponse = Geoip.cityDatabase.tryCity(ipAddress).getOrNull()
 * ```
 *
 * @param ipAddress 조회할 IP 주소
 * @return CityResponse를 포함한 Optional 객체 (찾지 못하면 empty)
 */
fun DatabaseReader.tryFindCity(ipAddress: InetAddress): Result<CityResponse> =
    runCatching { this.city(ipAddress) }

/**
 * IP 주소로부터 Country 정보를 조회합니다.
 *
 * ```
 * val countryResponse = Geoip.countryDatabase.tryCountry(ipAddress).getOrNull()
 * ```
 *
 * @param ipAddress 조회할 IP 주소
 * @return CountryResponse를 포함한 Optional 객체 (찾지 못하면 empty)
 */
fun DatabaseReader.tryFindCountry(ipAddress: InetAddress): Result<CountryResponse> =
    runCatching { this.country(ipAddress) }
