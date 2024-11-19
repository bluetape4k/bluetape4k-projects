package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.debug
import okhttp3.Interceptor
import okhttp3.Response

/**
 * [okhttp3.OkHttpClient] 실행 시, 요청과 응답 정보를 [logger]에 출력하는 [Interceptor]입니다.
 *
 * ```
 * val client = OkHttpClient.Builder()
 *    .addInterceptor(LoggingInterceptor(logger))
 *    .build()
 * ```
 *
 * @property logger 로그를 출력할 [org.slf4j.Logger]
 */
class LoggingInterceptor private constructor(
    private val logger: org.slf4j.Logger,
): Interceptor {

    companion object {
        /**
         * [okhttp3.OkHttpClient] 실행 시, 요청과 응답 정보를 [logger]에 출력하는 [Interceptor]입니다.
         *
         * @param logger 로그를 출력할 [org.slf4j.Logger]
         */
        @JvmStatic
        operator fun invoke(logger: org.slf4j.Logger): LoggingInterceptor {
            return LoggingInterceptor(logger)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.debug {
            "Sending request. url=${request.url}, connection=${chain.connection()}, headers=${request.headers}"
        }
        val startMillis = System.currentTimeMillis()

        // 실제 작업
        val response = chain.proceed(request)

        val elapsedMillis = System.currentTimeMillis() - startMillis
        logger.debug {
            "Receive response. url=${response.request.url}, elapsed=${elapsedMillis} msec. headers=${response.headers}"
        }

        return response
    }
}
