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
 * ```kotlin
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
         * [CacheControl] 인스턴스를 받아 [CachingRequestInterceptor]를 생성합니다.
         *
         * @param cacheControl 적용할 [CacheControl]
         * @return [CachingRequestInterceptor] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            cacheControl: CacheControl = okhttp3CacheControlOf(),
        ): CachingRequestInterceptor {
            return CachingRequestInterceptor(cacheControl)
        }

        /**
         * 캐시 제어 파라미터를 직접 지정해 [CachingRequestInterceptor]를 생성합니다.
         *
         * @param maxAgeInSeconds 캐시 최대 유효 시간 (초)
         * @param maxStaleInSeconds 만료된 캐시를 허용하는 최대 시간 (초)
         * @param minFreshInSeconds 최소 신선도 유지 시간 (초)
         * @param onlyIfCached 캐시에 있을 때만 응답 허용
         * @param noCache 캐시 사용 금지
         * @param noStore 캐시 저장 금지
         * @param noTransform 캐시 변환 금지
         * @param immutable 캐시 불변 처리
         * @return [CachingRequestInterceptor] 인스턴스
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
     * 요청에 Cache-Control 헤더가 없을 때만 [cacheControl]을 추가합니다.
     * 이미 Cache-Control 헤더가 있는 요청은 그대로 전달합니다.
     *
     * @param chain [Interceptor.Chain] 인스턴스
     * @return [Response]
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
