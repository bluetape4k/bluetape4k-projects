package io.bluetape4k.http.okhttp3

import io.bluetape4k.logging.debug
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that logs request and response diagnostics.
 *
 * ## Contract
 * - Logs request URL, connection, and redacted request headers before execution.
 * - Logs response URL, elapsed time, and redacted response headers after execution.
 * - Redacts credential-bearing headers by default and supports additional project-specific names.
 *
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *    .addInterceptor(LoggingInterceptor(logger))
 *    .build()
 * ```
 *
 * @property logger Logger that receives diagnostics.
 */
class LoggingInterceptor private constructor(
    private val logger: org.slf4j.Logger,
    private val additionalSensitiveHeaderNames: Set<String> = emptySet(),
): Interceptor {

    companion object {
        /**
         * Creates a logging interceptor with the default sensitive header redaction policy.
         *
         * @param logger Logger that receives request and response diagnostics.
         */
        @JvmStatic
        operator fun invoke(logger: org.slf4j.Logger): LoggingInterceptor {
            return LoggingInterceptor(logger)
        }

        /**
         * Creates a logging interceptor with project-specific sensitive headers.
         *
         * @param logger Logger that receives request and response diagnostics.
         * @param additionalSensitiveHeaderNames Extra header names whose values must be redacted.
         */
        @JvmStatic
        operator fun invoke(
            logger: org.slf4j.Logger,
            additionalSensitiveHeaderNames: Set<String>,
        ): LoggingInterceptor {
            return LoggingInterceptor(logger, additionalSensitiveHeaderNames)
        }
    }

    /**
     * Logs redacted request/response diagnostics around [chain] execution.
     *
     * @param chain OkHttp interceptor chain.
     * @return The response returned by the next interceptor or network call.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.debug {
            "Sending request. url=${request.url}, connection=${chain.connection()}, " +
                    "headers=${request.headers.toRedactedString(additionalSensitiveHeaderNames)}"
        }
        val startMillis = System.currentTimeMillis()

        // 실제 작업
        val response = chain.proceed(request)

        val elapsedMillis = System.currentTimeMillis() - startMillis
        logger.debug {
            "Receive response. url=${response.request.url}, elapsed=${elapsedMillis} msec. " +
                    "headers=${response.headers.toRedactedString(additionalSensitiveHeaderNames)}"
        }

        return response
    }
}
