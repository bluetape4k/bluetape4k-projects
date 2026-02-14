package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.KLogging
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp3 [Response]에 캐싱 설정을 추가하는 Interceptor 입니다.
 *
 * OkHttp3Client Builder에 `addNetworkInterceptor` 에 추가해야 합니다.
 *
 * ```
 * val client = OkHttpClient.Builder()
 *   .addNetworkInterceptor(CachingResponseInterceptor())
 *   .build()
 * ```
 */
class CachingResponseInterceptor private constructor(
    private val cacheControl: CacheControl,
): Interceptor {

    companion object: KLogging() {
        /**
         * HTTP 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(
            cacheControl: CacheControl = okhttp3CacheControlOf(),
        ): CachingResponseInterceptor {
            return CachingResponseInterceptor(cacheControl)
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
        ): CachingResponseInterceptor {
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
            return CachingResponseInterceptor(cacheControl)
        }
    }

    /**
     * HTTP 처리에서 `intercept` 함수를 제공합니다.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.header("Cache-Control")?.isNotBlank() == true) {
            return response
        }

        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}
