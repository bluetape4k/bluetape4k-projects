package io.bluetape4k.geocode.google

import com.google.maps.GeoApiContext
import com.google.maps.OkHttpRequestHandler

/**
 * Google Maps API를 사용하기 위한 [GeoApiContext]를 생성합니다.
 *
 * ```
 * val geoApiContext = geoApiContext {
 *    apiKey = "YOUR_API"
 *    connectTimeout = 1000
 *    readTimeout = 1000
 *    writeTimeout = 1000
 *    queryRateLimit = 10
 * }
 * ```
 *
 * @param requestHandlerBuilder [GeoApiContext.RequestHandler.Builder] 인스턴스
 * @param builder [GeoApiContext.Builder] 초기화 람다
 * @return [GeoApiContext] 인스턴스
 */
inline fun geoApiContext(
    requestHandlerBuilder: GeoApiContext.RequestHandler.Builder = OkHttpRequestHandler.Builder(),
    @BuilderInference builder: GeoApiContext.Builder.() -> Unit,
): GeoApiContext =
    GeoApiContext
        .Builder(requestHandlerBuilder)
        .apply(builder)
        .build()
