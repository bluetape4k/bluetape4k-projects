package io.bluetape4k.geoip2.finder

import io.bluetape4k.geoip2.Address
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.geoip2.tryFindCountry
import io.bluetape4k.logging.KLogging
import java.net.InetAddress

/**
 * IP 주소를 Country 단위 주소 정보로 조회하는 구현체입니다.
 *
 * ## 동작/계약
 * - [Geoip.countryDatabase]를 사용해 Country 응답을 조회합니다.
 * - 조회 성공 시 [Address.fromCountry] 결과를 반환합니다.
 * - 조회 실패/미존재는 null로 반환합니다.
 *
 * ```kotlin
 * val address = GeoipCountryFinder().findAddress(InetAddress.getByName("8.8.8.8"))
 * // address == null || address.countryIsoCode != null
 * ```
 */
class GeoipCountryFinder: GeoipFinder {

    companion object: KLogging()

    /**
     * IP Address 정보로부터 국가 단위의 행정 주소 [Address] 를 찾습니다.
     *
     * ## 동작/계약
     * - [tryFindCountry] 결과가 성공이면 Country 응답을 [Address]로 변환합니다.
     * - 실패 결과는 `getOrNull()`로 null 처리합니다.
     *
     * ```kotlin
     * val ipAddress = InetAddress.getByName(host)
     * val address = countryFinder.findAddress(ipAddress)
     * // address == null || address.continent != null
     * ```
     *
     * @param ipAddress 찾을 IP Address
     * @return DB에서 찾은 주소 정보 ([Address]) 또는 null
     */
    override fun findAddress(ipAddress: InetAddress): Address? {
        return Geoip.countryDatabase
            .tryFindCountry(ipAddress)
            .map { response -> Address.fromCountry(ipAddress, response) }
            .getOrNull()
    }
}
