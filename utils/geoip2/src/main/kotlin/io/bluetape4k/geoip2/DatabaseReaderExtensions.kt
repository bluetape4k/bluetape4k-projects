package io.bluetape4k.geoip2

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import com.maxmind.geoip2.model.CountryResponse
import java.net.InetAddress

/**
 * IP 주소로 City 정보를 조회하고 결과를 [Result]로 반환합니다.
 *
 * ## 동작/계약
 * - [DatabaseReader.city] 호출 성공 시 `Result.success`를 반환합니다.
 * - 조회 실패 예외는 `Result.failure`에 담아 반환합니다.
 * - 리더/캐시 상태 등 reader 내부 상태는 변경하지 않습니다.
 *
 * ```kotlin
 * val cityResponse = Geoip.cityDatabase.tryFindCity(ipAddress).getOrNull()
 * // cityResponse != null (조회 성공 경로)
 * ```
 *
 * @param ipAddress 조회할 IP 주소
 * @return CityResponse를 포함한 Optional 객체 (찾지 못하면 empty)
 */
fun DatabaseReader.tryFindCity(ipAddress: InetAddress): Result<CityResponse> =
    runCatching { this.city(ipAddress) }

/**
 * IP 주소로 Country 정보를 조회하고 결과를 [Result]로 반환합니다.
 *
 * ## 동작/계약
 * - [DatabaseReader.country] 호출 성공 시 `Result.success`를 반환합니다.
 * - 조회 실패 예외는 `Result.failure`에 담아 반환합니다.
 * - reader 인스턴스 상태를 직접 변경하지 않습니다.
 *
 * ```kotlin
 * val countryResponse = Geoip.countryDatabase.tryFindCountry(ipAddress).getOrNull()
 * // countryResponse != null (조회 성공 경로)
 * ```
 *
 * @param ipAddress 조회할 IP 주소
 * @return CountryResponse를 포함한 Optional 객체 (찾지 못하면 empty)
 */
fun DatabaseReader.tryFindCountry(ipAddress: InetAddress): Result<CountryResponse> =
    runCatching { this.country(ipAddress) }
