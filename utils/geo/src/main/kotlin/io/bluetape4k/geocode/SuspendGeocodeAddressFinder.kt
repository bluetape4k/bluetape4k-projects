package io.bluetape4k.geocode

/**
 * Geocode (위경도) 로부터 Coroutine 방식으로 주소를 찾습니다. 보통 City 범위까지 찾을 수 있습니다.
 *
 * ## 동작/계약
 * - suspend 방식으로 주소 조회를 수행합니다.
 * - 취소/예외 전파 규칙은 구현체의 외부 API 호출 정책을 따릅니다.
 *
 * ```kotlin
 * val address = finder.suspendFindAddress(Geocode(37.5665, 126.9780), "en")
 * // address == null || address.city != null
 * ```
 */
interface SuspendGeocodeAddressFinder {
    /**
     * 위경도([geocode])에 해당하는 주소를 비동기 방식으로 찾습니다.
     *
     * ## 동작/계약
     * - [language]는 조회 서비스 언어 파라미터로 전달됩니다.
     * - 조회 실패/미존재 시 null 반환 또는 예외 전파는 구현체 정책을 따릅니다.
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보 (기본값: "ko")
     * @return 주소([Address]) 정보
     */
    suspend fun suspendFindAddress(geocode: Geocode, language: String = "ko"): Address?
}
