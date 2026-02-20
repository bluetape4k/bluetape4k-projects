package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.KLogging
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp3 Request 시 캐싱 관련 정보 추가하는 Interceptor 입니다.
 *
 * OkHttp3Client Builder에 `addInterceptor` 에 추가해야 합니다.
 *
 * ```
 * // add interceptor
 * val client = OkHttpClient.Builder()
 *  .addInterceptor(CachingRequestInterceptor())
 *  .build()
 * ```
 */
class CachingRequestInterceptor private constructor(
    private val cacheControl: CacheControl,
): Interceptor {

    companion object: KLogging() {
        /**
         * HTTP 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(
            cacheControl: CacheControl = okhttp3CacheControlOf(),
        ): CachingRequestInterceptor {
            return CachingRequestInterceptor(cacheControl)
        }

        /**
         * HTTP 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(
            maxAgeInSeconds: Int = 0,
            maxStaleInSeconds: Int = 0,
            minFreshInSeconds: Int = 0,
            onlyIfCached: Boolean = false,
            noCache: Boolean = false,
            noStore: Boolean = false,
            noTransform: Boolean = false,
            immutable: Boolean = false,
        ): CachingRequestInterceptor {
            val cacheControl = okhttp3CacheControlOf(
                maxAgeInSeconds = maxAgeInSeconds,
                maxStaleInSeconds = maxStaleInSeconds,
                minFreshInSeconds = minFreshInSeconds,
                onlyIfCached = onlyIfCached,
                noCache = noCache,
                noStore = noStore,
                noTransform = noTransform,
                immutable = immutable
            )
            return CachingRequestInterceptor(cacheControl)
        }
    }

    /**
     * HTTP 처리에서 `intercept` 함수를 제공합니다.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.header("Cache-Control").isNullOrBlank()) {
            return chain.proceed(request)
        }
        val requestWithCaching = request.newBuilder().cacheControl(cacheControl).build()

        return chain.proceed(requestWithCaching)
    }
}
