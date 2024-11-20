package io.bluetape4k.geocode.bing

import io.bluetape4k.geocode.Geocode
import io.bluetape4k.geocode.GeocodeAddressFinder
import io.bluetape4k.geocode.bing.BingMapModel.toBingAddress
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug

/**
 * Bing Maps API를 통해 위경도로 주소 정보를 찾습니다.
 *
 * 참고: [Bing Maps API](https://www.bingmapsportal.com/)
 */
class BingAddressFinder: GeocodeAddressFinder {

    companion object: KLogging()

    private val client by lazy {
        BingMapService.getBingMapFeignClient()
    }

    private val asyncClient by lazy {
        BingMapService.getBingMapFeignCoroutineClient()
    }

    /**
     * Bing Maps API를 통해 위경도로 주소 정보를 찾습니다.
     *
     * ```
     * val addressFinder = BingAddressFinder()
     * val geocode = Geocode(37.5665, 126.9780)
     * val address = addressFinder.findAddress(geocode, "ko")
     * ```
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
     * ```
     * val addressFinder = BingAddressFinder()
     * val geocode = Geocode(37.5665, 126.9780)
     * val address = addressFinder.findAddressAsync(geocode, "ko")
     * ```
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override suspend fun findAddressAsync(geocode: Geocode, language: String): BingAddress? {
        val location = asyncClient.locations(
            latitude = geocode.latitude.toDouble(),
            longitude = geocode.longitude.toDouble(),
        )
        log.debug { "location=$location" }
        return location.toBingAddress()
    }
}
