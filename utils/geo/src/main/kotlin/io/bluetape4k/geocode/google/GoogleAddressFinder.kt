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
 * ## 동작/계약
 * - 생성 시 [apiKey]가 blank면 [IllegalArgumentException]이 발생합니다.
 * - 동기/비동기 조회 모두 reverse geocode API를 호출해 첫 번째 결과만 사용합니다.
 * - 조회 실패는 동기에서는 예외, suspend에서는 [IOException]으로 전파됩니다.
 *
 * ```kotlin
 * val finder = GoogleAddressFinder(apiKey)
 * val address = finder.findAddress(Geocode(37.5665, 126.9780), "ko")
 * // address == null || address.formattedAddress != null
 * ```
 *
 * @param apiKey Google Map Api Key
 */
class GoogleAddressFinder private constructor(apiKey: String): GeocodeAddressFinder {

    companion object: KLoggingChannel() {
        @JvmStatic
        /**
         * API 키로 [GoogleAddressFinder]를 생성합니다.
         *
         * ## 동작/계약
         * - [apiKey]가 blank면 [IllegalArgumentException]이 발생합니다.
         */
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
     * ## 동작/계약
     * - reverse geocode 결과 배열의 첫 요소만 [GoogleAddress]로 변환합니다.
     * - 결과가 비어 있으면 null을 반환합니다.
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
     * ## 동작/계약
     * - 콜백 기반 API를 `suspendCancellableCoroutine`으로 래핑합니다.
     * - 코루틴 취소 시 요청 취소를 시도합니다.
     * - 실패 콜백은 [IOException]으로 변환해 예외 전파합니다.
     *
     * ```kotlin
     * val address = finder.suspendFindAddress(Geocode(37.5665, 126.9780), "ko")
     * // address == null || address.country != null
     * ```
     *
     * @param geocode 위경도 정보
     * @param language 언어 정보
     * @return 주소 정보 또는 null
     */
    override suspend fun suspendFindAddress(geocode: Geocode, language: String): Address? {
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
