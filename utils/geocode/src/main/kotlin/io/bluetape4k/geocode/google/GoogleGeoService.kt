package io.bluetape4k.geocode.google

import com.google.maps.GeoApiContext
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex

/**
 * Google Maps API를 사용하기 위한 Api Key, [GeoApiContext]를 제공합니다.
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
     */
    val context: GeoApiContext by lazy {
        geoApiContext {
            apiKey(apiKey)
            maxRetries(3)
        }
    }
}
