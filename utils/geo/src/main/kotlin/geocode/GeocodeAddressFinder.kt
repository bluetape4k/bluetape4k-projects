package io.bluetape4k.geocode

/**
 * Geocode (위경도) 로부터 주소를 찾습니다. 보통 City 범위까지 찾을 수 있습니다.
 *
 * ## 동작/계약
 * - 동기 방식으로 [Geocode]에 해당하는 [Address]를 조회합니다.
 * - 조회 실패/미존재 시 null 반환 여부는 구현체 정책을 따릅니다.
 *
 * ```kotlin
 * val address = finder.findAddress(Geocode(37.5665, 126.9780), "ko")
 * // address == null || address.country != null
 * ```
 */
interface GeocodeAddressFinder: SuspendGeocodeAddressFinder {

    /**
     * 위경도([geocode])에 해당하는 주소를 찾습니다.
     *
     * ## 동작/계약
     * - [language]는 외부 API 요청 시 로케일 힌트로 사용됩니다.
     * - 실패/미존재 시 null을 반환할 수 있습니다.
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보 (기본값: "ko")
     * @return 주소([Address]) 정보 또는 null
     */
    fun findAddress(geocode: Geocode, language: String = "ko"): Address?

}
