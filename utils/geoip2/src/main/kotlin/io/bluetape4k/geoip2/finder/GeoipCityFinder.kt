package io.bluetape4k.geoip2.finder

import io.bluetape4k.geoip2.Address
import io.bluetape4k.geoip2.Geoip
import io.bluetape4k.geoip2.tryFindCity
import io.bluetape4k.logging.KLogging
import java.net.InetAddress

/**
 * IP 주소를 City 단위 주소 정보로 조회하는 구현체입니다.
 *
 * ## 동작/계약
 * - [Geoip.cityDatabase]를 사용해 City 응답을 조회합니다.
 * - 조회 성공 시 [Address.fromCity] 결과를 반환합니다.
 * - 조회 실패/미존재는 null로 반환합니다.
 *
 * ```kotlin
 * val address = GeoipCityFinder().findAddress(InetAddress.getByName("8.8.8.8"))
 * // address == null || address.city != null
 * ```
 */
class GeoipCityFinder: GeoipFinder {

    companion object: KLogging()

    /**
     * IP Address 정보로부터 City 단위의 행정 주소 [Address] 를 찾습니다.
     *
     * ## 동작/계약
     * - [tryFindCity] 결과가 성공이면 City 응답을 [Address]로 변환합니다.
     * - 실패 결과는 `getOrNull()`로 null 처리합니다.
     *
     * ```kotlin
     * val ipAddress = InetAddress.getByName(host)
     * val address = cityFinder.findAddress(ipAddress)
     * // address == null || address.country != null
     * ```
     *
     * @param ipAddress 찾을 IP Address
     * @return DB에서 찾은 주소 정보 ([Address]) 또는 null
     */
    override fun findAddress(ipAddress: InetAddress): Address? {
        return Geoip.cityDatabase
            .tryFindCity(ipAddress)
            .map { response -> Address.fromCity(ipAddress, response) }
            .getOrNull()
    }
}
