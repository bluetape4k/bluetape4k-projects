package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.HttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.core5.util.TimeValue

/**
 * Creates a [HttpRequestRetryStrategy] backed by [DefaultHttpRequestRetryStrategy].
 *
 * Retries on transient IOExceptions and retriable HTTP status codes (503, 429, etc.)
 * with a fixed interval between attempts.
 *
 * ```kotlin
 * val retry = defaultRetryStrategy()                   // 3 retries, 1 s interval
 * val retry = defaultRetryStrategy(maxRetries = 5,
 *     retryInterval = TimeValue.ofSeconds(2))
 * val client = productionHttpClientOf(retryStrategy = retry)
 * ```
 *
 * @param maxRetries maximum number of retry attempts (default: 3)
 * @param retryInterval wait interval between retries (default: 1 s)
 * @return configured [HttpRequestRetryStrategy]
 */
fun defaultRetryStrategy(
    maxRetries: Int = 3,
    retryInterval: TimeValue = TimeValue.ofSeconds(1),
): HttpRequestRetryStrategy = DefaultHttpRequestRetryStrategy(maxRetries, retryInterval)
