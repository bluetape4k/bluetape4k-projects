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
) : Interceptor {
    private val cacheControlHeader: String = cacheControl.toString()

    companion object : KLogging() {
        /**
         * [CacheControl] 인스턴스를 받아 [CachingResponseInterceptor]를 생성합니다.
         *
         * @param cacheControl 적용할 [CacheControl]
         * @return [CachingResponseInterceptor] 인스턴스
         */
        @JvmStatic
        operator fun invoke(cacheControl: CacheControl = okhttp3CacheControlOf()): CachingResponseInterceptor =
            CachingResponseInterceptor(cacheControl)

        /**
         * 캐시 제어 파라미터를 직접 지정해 [CachingResponseInterceptor]를 생성합니다.
         *
         * @param maxAgeInSeconds 캐시 최대 유효 시간 (초)
         * @param maxStaleInSeconds 만료된 캐시를 허용하는 최대 시간 (초)
         * @param minFreshInSeconds 최소 신선도 유지 시간 (초)
         * @param onlyIfCached 캐시에 있을 때만 응답 허용
         * @param noCache 캐시 사용 금지
         * @param noStore 캐시 저장 금지
         * @param noTransform 캐시 변환 금지
         * @param immutable 캐시 불변 처리
         * @return [CachingResponseInterceptor] 인스턴스
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
            val cacheControl =
                okhttp3CacheControlOf(
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
     * 응답에 Cache-Control 헤더가 없을 때만 [cacheControl]을 추가합니다.
     * 이미 Cache-Control 헤더가 있는 응답은 그대로 반환합니다.
     *
     * @param chain [Interceptor.Chain] 인스턴스
     * @return Cache-Control 헤더가 포함된 [Response]
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.header("Cache-Control")?.isNotBlank() == true) {
            return response
        }

        return response
            .newBuilder()
            .header("Cache-Control", cacheControlHeader)
            .build()
    }
}
