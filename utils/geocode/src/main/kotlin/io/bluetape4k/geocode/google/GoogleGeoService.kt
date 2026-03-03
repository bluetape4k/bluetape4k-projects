package io.bluetape4k.geocode.google

import com.google.maps.GeoApiContext
import io.bluetape4k.geocode.google.GoogleGeoService.apiKey
import io.bluetape4k.geocode.google.GoogleGeoService.context
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex

/**
 * Google Maps API를 사용하기 위한 Api Key, [GeoApiContext]를 제공합니다.
 *
 * ## 동작/계약
 * - [apiKey]는 리소스 파일(`GoogleGeocodeApi.key`)에서 lazy 로딩합니다.
 * - [context]는 lazy 초기화 후 동일 인스턴스를 재사용합니다.
 *
 * ```kotlin
 * val context = GoogleGeoService.context
 * // context != null
 * ```
 */
object GoogleGeoService: KLogging() {

    /**
     * 구글 맵 Geocode API 를 사용하기 위해서는 API Key 를 생성해야 합니다.
     * 현재 sunghyouk.bae@gmail.com 으로 계정을 만들었습니다.
     *
     * 참고: [API 사용하기](https://developers.google.com/maps/documentation/javascript/get-api-key?hl=ko)
     * 참고: [Debop의 Google Map Reverse Geocode API Key](https://console.cloud.google.com/apis/credentials/key/2d935790-3118-4d0c-9468-999e0c3aa64f?hl=ko&project=data-rider-388411)
     */
    internal val apiKey: String by lazy {
        Resourcex.getString("GoogleGeocodeApi.key")
    }

    /**
     * Google Maps API를 사용하기 위한 [GeoApiContext]를 생성합니다.
     *
     * ## 동작/계약
     * - [apiKey]와 재시도 정책(`maxRetries(3)`)을 적용한 컨텍스트를 생성합니다.
     */
    val context: GeoApiContext by lazy {
        geoApiContext {
            apiKey(apiKey)
            maxRetries(3)
        }
    }
}
