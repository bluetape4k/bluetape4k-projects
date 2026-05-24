package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy
import org.apache.hc.core5.util.TimeValue

/**
 * Creates a [ConnectionKeepAliveStrategy] with a fallback duration for servers that omit
 * the `Keep-Alive` header.
 *
 * Falls back to [fallbackDuration] when the header is absent (HC5 signals this as a negative value).
 *
 * ```kotlin
 * val strategy = defaultKeepAliveStrategy()                          // 60 s fallback
 * val strategy = defaultKeepAliveStrategy(TimeValue.ofSeconds(30))  // 30 s fallback
 * val client = productionHttpClientOf(keepAliveStrategy = strategy)
 * ```
 *
 * @param fallbackDuration duration used when the server does not specify Keep-Alive (default: 60 s)
 * @return [ConnectionKeepAliveStrategy] with fallback applied
 */
fun defaultKeepAliveStrategy(
    fallbackDuration: TimeValue = TimeValue.ofSeconds(60),
): ConnectionKeepAliveStrategy = ConnectionKeepAliveStrategy { response, context ->
    val duration = DefaultConnectionKeepAliveStrategy.INSTANCE.getKeepAliveDuration(response, context)
    if (duration.duration < 0) fallbackDuration else duration
}
