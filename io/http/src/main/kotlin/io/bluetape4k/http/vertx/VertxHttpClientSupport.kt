package io.bluetape4k.http.vertx

import io.bluetape4k.vertx.currentVertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.kotlin.core.http.httpClientOptionsOf

/**
 * Default Vert.x [HttpClientOptions]
 */
@JvmField
val defaultVertxHttpClientOptions: HttpClientOptions = httpClientOptionsOf(
    protocolVersion = HttpClientOptions.DEFAULT_PROTOCOL_VERSION,
    keepAlive = true,
    useAlpn = true,
    trustAll = true,
    logActivity = true,
    tryUsePerFrameWebSocketCompression = true,
    tryUsePerMessageWebSocketCompression = true
)

/**
 * Default Vert.x [HttpClient]
 */
val defaultVertxHttpClient: HttpClient by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
    vertxHttpClientOf(defaultVertxHttpClientOptions)
}

/**
 * Create a new Vert.x [HttpClient] with the given [options].
 *
 * @param options [HttpClientOptions]
 * @return [HttpClient]
 */
fun vertxHttpClientOf(options: HttpClientOptions = defaultVertxHttpClientOptions): HttpClient {
    return currentVertx().createHttpClient(options)
}
