package io.bluetape4k.geoip2.finder

import io.bluetape4k.geoip2.Address
import java.net.InetAddress

/**
 * IP 주소를 행정 주소 정보로 조회하는 공통 계약입니다.
 *
 * ## 동작/계약
 * - 조회 성공 시 [Address], 실패/미존재 시 null을 반환합니다.
 * - 네트워크/DB 예외 처리 방식은 구현체 정책에 따릅니다.
 *
 * ```kotlin
 * val address = finder.findAddress(InetAddress.getByName("8.8.8.8"))
 * // address?.country != null
 * ```
 */
interface GeoipFinder {

    /**
     * IP Address 정보로부터 행정 주소 [Address] 를 찾습니다.
     *
     * ## 동작/계약
     * - [ipAddress] 기준으로 단일 주소 정보를 조회합니다.
     * - 조회 결과가 없거나 실패하면 null을 반환할 수 있습니다.
     *
     * ```kotlin
     * val ipAddress = InetAddress.getByName(host)
     * val address = cityFinder.findAddress(ipAddress)
     * // address == null || address.ipAddress != null
     * ```
     *
     * @param ipAddress 찾을 IP Address
     * @return DB에서 찾은 주소 정보 ([Address]) 또는 null
     */
    fun findAddress(ipAddress: InetAddress): Address?
}
