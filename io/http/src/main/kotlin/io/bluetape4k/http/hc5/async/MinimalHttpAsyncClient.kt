package io.bluetape4k.http.hc5.async

import io.bluetape4k.coroutines.support.suspendAwait
import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.SystemDefaultDnsResolver
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.async.MinimalH2AsyncClient
import org.apache.hc.client5.http.impl.async.MinimalHttpAsyncClient
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.config.Http1Config
import org.apache.hc.core5.http.nio.AsyncClientEndpoint
import org.apache.hc.core5.http.nio.ssl.TlsStrategy
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http2.config.H2Config
import org.apache.hc.core5.reactor.IOReactorConfig

/**
 * 기본 설정으로 [MinimalHttpAsyncClient]를 생성합니다.
 */
@JvmField
val defaultMinimalHttpAsyncClient: MinimalHttpAsyncClient = HttpAsyncClients.createMinimal()

/**
 * 기본 설정으로 [MinimalH2AsyncClient]를 생성합니다.
 */
@JvmField
val defaultMinimalH2AsyncClient: MinimalH2AsyncClient = HttpAsyncClients.createHttp2Minimal()

/**
 * 고급 HTTP protocol 기능 없이 간단한 HTTP/1.1과 HTTP/2 메시진 전송용 [MinimalHttpAsyncClient] 인스턴스를 생성합니다.
 *
 * ```
 * val client = minimalHttpAsyncClient(
 *      h2config = H2Config.custom().build(),
 *      h1config = Http1Config.custom().build(),
 *      ioReactorConfig = IOReactorConfig.custom().build(),
 *      connMgr = asyncClientConnectionManager {
 *          setMaxTotal(100)
 *          setDefaultMaxPerRoute(10)
 *          setValidateAfterInactivity(1000)
 *      }
 * }
 * ```
 *
 * @param h2config [H2Config] 설정
 * @param h1config [Http1Config] 설정
 * @param ioReactorConfig [IOReactorConfig] 설정
 * @param connMgr [AsyncClientConnectionManager] 설정
 * @return [MinimalHttpAsyncClient] 인스턴스
 */
fun minimalHttpAsyncClientOf(
    h2config: H2Config = H2Config.DEFAULT,
    h1config: Http1Config = Http1Config.DEFAULT,
    ioReactorConfig: IOReactorConfig = IOReactorConfig.DEFAULT,
    connMgr: AsyncClientConnectionManager = defaultAsyncClientConnectionManager,
): MinimalHttpAsyncClient {
    return HttpAsyncClients.createMinimal(h2config, h1config, ioReactorConfig, connMgr)
}

/**
 * 고급 HTTP protocol 기능 없이 간단한 HTTP/1.1과 HTTP/2 메시진 전송용 [MinimalH2AsyncClient] 인스턴스를 생성합니다.
 *
 * ```
 * val client = minimalH2AsyncClientOf(
 *      h2config = H2Config.custom().build(),
 *      ioReactorConfig = IOReactorConfig.custom().build(),
 *      dnsResolver = SystemDefaultDnsResolver.INSTANCE,
 *      tlsStrategy = DefaultClientTlsStrategy.getDefault(),
 * }
 * ```
 *
 * @param h2config [H2Config] 설정
 * @param ioReactorConfig [IOReactorConfig] 설정
 * @param dnsResolver [DnsResolver] 설정
 * @param tlsStrategy [TlsStrategy] 설정
 * @return [MinimalH2AsyncClient] 인스턴스
 */
fun minimalH2AsyncClientOf(
    h2config: H2Config,
    ioReactorConfig: IOReactorConfig = IOReactorConfig.DEFAULT,
    dnsResolver: DnsResolver = SystemDefaultDnsResolver.INSTANCE,
    tlsStrategy: TlsStrategy = DefaultClientTlsStrategy.createDefault(),
): MinimalH2AsyncClient {
    return HttpAsyncClients.createHttp2Minimal(h2config, ioReactorConfig, dnsResolver, tlsStrategy)
}

/**
 * Coroutines 환경에서 [MinimalHttpAsyncClient.lease]를 수행합니다.
 *
 * @param host [HttpHost] 호스트 정보
 * @param context [HttpContext] 설정
 * @param callback [FutureCallback] 콜백
 * @return [AsyncClientEndpoint] 인스턴스
 */
@Deprecated("use suspendLease instead", replaceWith = ReplaceWith("suspendLease(host,context,callback)"))
suspend inline fun MinimalHttpAsyncClient.leaseSuspending(
    host: HttpHost,
    context: HttpContext = HttpClientContext.create(),
    @BuilderInference callback: FutureCallback<AsyncClientEndpoint>? = null,
): AsyncClientEndpoint {
    return lease(host, context, callback).suspendAwait()
}

/**
 * Coroutines 환경에서 [MinimalHttpAsyncClient.lease]를 수행합니다.
 *
 * @param host [HttpHost] 호스트 정보
 * @param context [HttpContext] 설정
 * @param callback [FutureCallback] 콜백
 * @return [AsyncClientEndpoint] 인스턴스
 */
suspend inline fun MinimalHttpAsyncClient.suspendLease(
    host: HttpHost,
    context: HttpContext = HttpClientContext.create(),
    @BuilderInference callback: FutureCallback<AsyncClientEndpoint>? = null,
): AsyncClientEndpoint {
    return lease(host, context, callback).suspendAwait()
}
