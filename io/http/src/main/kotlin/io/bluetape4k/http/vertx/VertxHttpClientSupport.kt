package io.bluetape4k.http.vertx

import io.bluetape4k.vertx.defaultVertx
import io.bluetape4k.vertx.currentVertx
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.kotlin.core.http.httpClientOptionsOf
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val defaultVertxHttpClientLock = ReentrantLock()
@Volatile
private var defaultVertxHttpClientRef: HttpClient? = null

/**
 * 기본 Vert.x [HttpClientOptions]입니다.
 *
 * Security note: `trustAll = false` keeps TLS certificate verification enabled.
 * Use custom options with `trustAll = true` only for controlled test environments.
 */
@JvmField
val defaultVertxHttpClientOptions: HttpClientOptions = httpClientOptionsOf(
    protocolVersion = HttpClientOptions.DEFAULT_PROTOCOL_VERSION,
    keepAlive = true,
    useAlpn = true,
    trustAll = false,
    logActivity = false,
)

/**
 * 관리되는 기본 Vert.x [HttpClient]입니다.
 *
 * ## Behaviour / Contract
 * - The client is created lazily from `bluetape4k-vertx`'s managed [defaultVertx].
 * - The client is reused across calls to this property.
 * - shutdown 중 관리되는 기본 Vert.x를 닫기 전에 [closeDefaultVertxHttpClient]를 호출합니다.
 *
 * ```kotlin
 * val client = defaultVertxHttpClient
 * // Reused managed client with keep-alive, ALPN, and TLS verification enabled.
 * ```
 */
val defaultVertxHttpClient: HttpClient
    get() = defaultVertxHttpClientLock.withLock {
        defaultVertxHttpClientRef ?: vertxHttpClientOf(defaultVertx(), defaultVertxHttpClientOptions)
            .also { defaultVertxHttpClientRef = it }
    }

/**
 * 관리되는 기본 Vert.x [HttpClient]가 생성되어 있으면 닫고 지웁니다.
 */
fun closeDefaultVertxHttpClient(): Future<Void> {
    val client = defaultVertxHttpClientLock.withLock {
        defaultVertxHttpClientRef.also { defaultVertxHttpClientRef = null }
    }

    return client?.close() ?: Future.succeededFuture()
}

/**
 * Creates a Vert.x [HttpClient] from the current Vert.x context owner or managed default Vert.x.
 *
 * For lifecycle-sensitive code, prefer [vertxHttpClientOf] with an explicit [Vertx] owner.
 *
 * ```kotlin
 * val options = httpClientOptionsOf(
 *     keepAlive = true,
 *     useAlpn = true,
 *     trustAll = false,
 * )
 * val client = vertxHttpClientOf(options)
 * ```
 *
 * @param options [HttpClientOptions]
 * @return [HttpClient]
 */
fun vertxHttpClientOf(options: HttpClientOptions = defaultVertxHttpClientOptions): HttpClient {
    return vertxHttpClientOf(currentVertx(), options)
}

/**
 * Creates a Vert.x [HttpClient] owned by the supplied [vertx] instance.
 *
 * Use this overload when the caller owns the Vert.x lifecycle explicitly.
 */
fun vertxHttpClientOf(
    vertx: Vertx,
    options: HttpClientOptions = defaultVertxHttpClientOptions,
): HttpClient {
    return vertx.createHttpClient(options)
}
