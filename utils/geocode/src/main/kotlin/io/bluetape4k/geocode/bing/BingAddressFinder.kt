package io.bluetape4k.geocode.bing

import io.bluetape4k.geocode.Geocode
import io.bluetape4k.geocode.GeocodeAddressFinder
import io.bluetape4k.geocode.bing.BingMapModel.toBingAddress
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug

/**
 * Bing Maps API를 통해 위경도로 주소 정보를 찾습니다.
 *
 * 참고: [Bing Maps API](https://www.bingmapsportal.com/)
 *
 * ## 동작/계약
 * - 동기 경로는 예외를 `runCatching`으로 감싸 null로 변환합니다.
 * - 코루틴 경로는 원 예외를 호출자에게 전파합니다.
 * - 응답의 첫 리소스만 [BingAddress]로 매핑합니다.
 *
 * ```kotlin
 * val finder = BingAddressFinder()
 * val address = finder.findAddress(Geocode(37.5665, 126.9780), "ko")
 * // address == null || address.country != null
 * ```
 */
class BingAddressFinder: GeocodeAddressFinder {

    companion object: KLoggingChannel()

    private val client by lazy {
        BingMapService.getBingMapFeignClient()
    }

    private val asyncClient by lazy {
        BingMapService.getBingMapFeignCoroutineClient()
    }

    /**
     * Bing Maps API를 통해 위경도로 주소 정보를 찾습니다.
     *
     * ## 동작/계약
     * - API 호출 성공 시 [BingMapModel.toBingAddress] 결과를 반환합니다.
     * - 실패는 null로 반환합니다.
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override fun findAddress(geocode: Geocode, language: String): BingAddress? {
        return runCatching {
            val location = client.locations(
                latitude = geocode.latitude.toDouble(),
                longitude = geocode.longitude.toDouble(),
            )
            log.debug { "location=$location" }
            location.toBingAddress()
        }.getOrNull()
    }

    /**
     * Bing Maps API를 통해 위경도로 주소 정보를 비동기 방식으로 찾습니다.
     *
     * ## 동작/계약
     * - suspend API 호출 성공 시 첫 리소스를 주소로 매핑합니다.
     * - 호출 예외는 그대로 전파됩니다.
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override suspend fun suspendFindAddress(geocode: Geocode, language: String): BingAddress? {
        val location = asyncClient.locations(
            latitude = geocode.latitude.toDouble(),
            longitude = geocode.longitude.toDouble(),
        )
        log.debug { "location=$location" }
        return location.toBingAddress()
    }
}
