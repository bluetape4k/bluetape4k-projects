package io.bluetape4k.geocode.bing

import feign.Feign
import feign.Headers
import feign.Logger
import feign.Param
import feign.RequestLine
import feign.hc5.ApacheHttp5Client
import feign.hc5.AsyncApacheHttp5Client
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.client
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.coroutines.client
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Resourcex

/**
 * Microsoft Bing Map Service API 를 사용하기 위한 Feign Client 입니다.
 *
 * [Bing Maps REST Services](https://learn.microsoft.com/en-us/bingmaps/rest-services/)
 *
 * ```
 * private val client = BingMapService.getBingMapFeignClient()
 *
 * val location = client.locations(37.5665, 126.9780)
 * val address = location.toBingAddress()
 * ```
 */
object BingMapService: KLogging() {

    const val BASE_URL = "http://dev.virtualearth.net"
    const val REST_V1 = "/REST/v1"

    @JvmStatic
    val apiKey: String by lazy { Resourcex.getString("BingGeocodeApi.key") }

    /**
     * Bing Map Service API 를 사용하기 위한 Feign Client 를 생성합니다.
     *
     * @return [Feign.Builder] 인스턴스
     */
    internal fun newFeignBuilder(): Feign.Builder {
        return feignBuilder {
            client(ApacheHttp5Client())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.BASIC)
        }
    }

    /**
     * Bing Map Service API 를 사용하기 위한 Feign Coroutine Client 를 생성합니다.
     *
     * @return [CoroutineFeign.CoroutineBuilder] 인스턴스
     */
    internal fun newFeignCoroutineBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncApacheHttp5Client())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.BASIC)
        }
    }

    /**
     * [BingMapApi]를 실행할 수 있는 Feign 용 Coroutine Client 입니다.
     *
     * @return [BingMapApi]의 Feign Client
     */
    fun getBingMapFeignClient(): BingMapApi =
        newFeignBuilder().client<BingMapApi>(BASE_URL)


    /**
     * [BingMapApi]를 실행할 수 있는 Feign 용 Coroutine Client 입니다.
     *
     * @return [BingMapApi]의 Feign Coroutine Client
     */
    fun getBingMapFeignCoroutineClient(): BingMapCoroutineApi =
        newFeignCoroutineBuilder().client<BingMapCoroutineApi>(BASE_URL)

    @Headers("Content-Type: application/json; charset=UTF-8")
    interface BingMapApi {

        /**
         * 위경도로 위치를 찾습니다.
         *
         * 참고: [Find a Location by Point](https://learn.microsoft.com/en-us/bingmaps/rest-services/locations/find-a-location-by-point)
         *
         * ```
         * private val client = BingMapService.getBingMapFeignClient()
         *
         * val location = client.locations(37.5665, 126.9780)
         * val address = location.toBingAddress()
         * ```
         *
         * @param latitude 위도
         * @param longitude 경도
         * @param apiKey Bing Map Service API Key
         * @return
         */
        @RequestLine("GET $REST_V1/Locations/{lat},{lon}?key={key}&incl=ciso2")
        fun locations(
            @Param("lat") latitude: Double,
            @Param("lon") longitude: Double,
            @Param("key") apiKey: String = BingMapService.apiKey,
        ): BingMapModel.Location
    }

    @Headers("Content-Type: application/json; charset=UTF-8")
    interface BingMapCoroutineApi {

        /**
         * 위경도로 위치를 찾습니다.
         *
         * 참고: [Find a Location by Point](https://learn.microsoft.com/en-us/bingmaps/rest-services/locations/find-a-location-by-point)
         *
         * ```
         * private val client = BingMapService.getBingMapFeignCoroutineClient()
         *
         * val location = client.locations(37.5665, 126.9780)
         * val address = location.toBingAddress()
         * ```
         *
         * @param latitude 위도
         * @param longitude 경도
         * @param apiKey Bing Map Service API Key
         * @return
         */
        @RequestLine("GET $REST_V1/Locations/{lat},{lon}?key={key}&incl=ciso2")
        suspend fun locations(
            @Param("lat") latitude: Double,
            @Param("lon") longitude: Double,
            @Param("key") apiKey: String = BingMapService.apiKey,
        ): BingMapModel.Location
    }
}
