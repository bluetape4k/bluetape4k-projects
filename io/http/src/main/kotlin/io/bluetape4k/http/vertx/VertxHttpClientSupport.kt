package io.bluetape4k.http.vertx

import io.bluetape4k.vertx.currentVertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.kotlin.core.http.httpClientOptionsOf

/**
 * 기본 Vert.x [HttpClientOptions] 입니다.
 *
 * 보안 참고: `trustAll = false`(기본값)로 설정되어 있어 TLS 인증서 검증이 활성화됩니다.
 * 테스트 환경에서 자체 서명 인증서를 사용하는 경우 `trustAll = true`로 별도 설정하세요.
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
 * 기본 Vert.x [HttpClient] 입니다.
 */
val defaultVertxHttpClient: HttpClient by lazy {
    vertxHttpClientOf(defaultVertxHttpClientOptions)
}

/**
 * 주어진 [options]으로 새 Vert.x [HttpClient]를 생성합니다.
 *
 * @param options [HttpClientOptions]
 * @return [HttpClient]
 */
fun vertxHttpClientOf(options: HttpClientOptions = defaultVertxHttpClientOptions): HttpClient {
    return currentVertx().createHttpClient(options)
}
