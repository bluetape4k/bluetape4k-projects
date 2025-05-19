package io.bluetape4k.geocode.google

import com.google.maps.GeocodingApi
import com.google.maps.PendingResult
import com.google.maps.model.GeocodingResult
import io.bluetape4k.geocode.Address
import io.bluetape4k.geocode.Geocode
import io.bluetape4k.geocode.GeocodeAddressFinder
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * Google Map Service의 위경도로 주소찾기 기능을 제공합니다.
 *
 * 참고: [Google Maps API](https://developers.google.com/maps/documentation/geocoding/start)
 *
 * @param apiKey Google Map Api Key
 */
class GoogleAddressFinder private constructor(apiKey: String): GeocodeAddressFinder {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun invoke(apiKey: String = GoogleGeoService.apiKey): GoogleAddressFinder {
            apiKey.requireNotBlank("apiKey")
            return GoogleAddressFinder(apiKey)
        }
    }

    private val context = geoApiContext {
        apiKey(apiKey)
        maxRetries(3)
    }

    /**
     * Google Maps API를 통해 위경도로 주소 정보를 찾습니다.
     *
     * ```
     * val addressFinder = GoogleAddressFinder(apiKey)
     * val geocode = Geocode(37.5665, 126.9780)
     * val address = addressFinder.findAddress(geocode, "ko")
     * ```
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override fun findAddress(geocode: Geocode, language: String): GoogleAddress? {
        val result = GeocodingApi
            .reverseGeocode(context, geocode.toLatLng())
            .apply { language(language) }
            .await()

        log.debug { "find address for geocode=$geocode, GeocodingResult=${result?.firstOrNull()}" }
        return result?.firstOrNull()?.toAddress()
    }

    /**
     * Google Maps API를 통해 위경도로 주소 정보를 비동기 방식으로 찾습니다.
     *
     * ```
     * val addressFinder = GoogleAddressFinder(apiKey)
     * val geocode = Geocode(37.5665, 126.9780)
     * runBlocking {
     *      val address = addressFinder.findAddressAsync(geocode, "ko")
     * }
     * ```
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override suspend fun findAddressAsync(geocode: Geocode, language: String): Address? {
        return suspendCancellableCoroutine { cont ->
            val request = GeocodingApi.reverseGeocode(context, geocode.toLatLng())
            request.language(language)
            request.setCallback(object: PendingResult.Callback<Array<out GeocodingResult>> {
                override fun onResult(result: Array<out GeocodingResult>?) {
                    log.debug { "find address for geocode=$geocode, GeocodingResult=${result?.firstOrNull()}" }
                    cont.resume(result?.firstOrNull()?.toAddress()) { _, _, _ -> request.cancel() }
                }

                override fun onFailure(e: Throwable?) {
                    log.warn(e) { "Fail to retrieve address. geocode=$geocode" }
                    cont.resumeWithException(IOException("Fail to retrieve address. geocode=$geocode", e))
                }
            })
            cont.invokeOnCancellation {
                runCatching { request.cancel() }
            }
        }
    }
}
