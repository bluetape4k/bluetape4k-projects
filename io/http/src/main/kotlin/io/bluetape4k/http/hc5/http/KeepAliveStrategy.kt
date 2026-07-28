package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy
import org.apache.hc.core5.util.TimeValue

/**
 * `Keep-Alive` header를 생략하는 server를 위해 fallback duration을 가진 [ConnectionKeepAliveStrategy]를 생성합니다.
 *
 * header가 없으면 HC5가 음수 duration으로 신호를 주므로 [fallbackDuration]으로 대체합니다.
 *
 * ```kotlin
 * val strategy = defaultKeepAliveStrategy()                          // 60 s fallback
 * val strategy = defaultKeepAliveStrategy(TimeValue.ofSeconds(30))  // 30 s fallback
 * val client = productionHttpClientOf(keepAliveStrategy = strategy)
 * ```
 *
 * @param fallbackDuration server가 Keep-Alive를 지정하지 않을 때 사용할 duration입니다. 기본값은 60초입니다.
 * @return fallback이 적용된 [ConnectionKeepAliveStrategy]입니다.
 */
fun defaultKeepAliveStrategy(
    fallbackDuration: TimeValue = TimeValue.ofSeconds(60),
): ConnectionKeepAliveStrategy = ConnectionKeepAliveStrategy { response, context ->
    val duration = DefaultConnectionKeepAliveStrategy.INSTANCE.getKeepAliveDuration(response, context)
    if (duration.duration < 0) fallbackDuration else duration
}
