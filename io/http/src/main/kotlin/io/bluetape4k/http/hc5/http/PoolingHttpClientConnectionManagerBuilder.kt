package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.DnsResolver
import org.apache.hc.client5.http.HttpRoute
import org.apache.hc.client5.http.SchemePortResolver
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.io.ManagedHttpClientConnection
import org.apache.hc.core5.function.Resolver
import org.apache.hc.core5.http.io.HttpConnectionFactory
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.pool.PoolConcurrencyPolicy
import org.apache.hc.core5.pool.PoolReusePolicy
import org.apache.hc.core5.util.TimeValue

/**
 * [PoolingHttpClientConnectionManager] 를 생성합니다.
 *
 * ```
 * val cm = poolingHttpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * ```
 */
inline fun poolingHttpClientConnectionManager(
    initializer: PoolingHttpClientConnectionManagerBuilder.() -> Unit,
): PoolingHttpClientConnectionManager =
    PoolingHttpClientConnectionManagerBuilder.create().apply(initializer).build()

/**
 * [PoolingHttpClientConnectionManager] 를 생성합니다.
 *
 * ```
 * val cm = poolingHttpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * ```
 */
fun poolingHttpClientConnectionManagerOf(): PoolingHttpClientConnectionManager =
    poolingHttpClientConnectionManager { }

/**
 * [PoolingHttpClientConnectionManager] 를 생성합니다.
 */
fun poolingHttpClientConnectionManagerOf(
    // socketFactoryRegistry: Registry<ConnectionSocketFactory> = defaultSocketFactoryRegistry,
    poolConcurrencyPolicy: PoolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT,
    poolReusePolicy: PoolReusePolicy = PoolReusePolicy.LIFO,
    timeToLive: TimeValue = TimeValue.NEG_ONE_MILLISECOND,
    schemePortResolver: SchemePortResolver? = null,
    dnsResolver: DnsResolver? = null,
    connFactory: HttpConnectionFactory<ManagedHttpClientConnection>? = null,
): PoolingHttpClientConnectionManager =
    poolingHttpClientConnectionManager {
        // .setSocketFactoryRegistry(socketFactoryRegistry)
        setPoolConcurrencyPolicy(poolConcurrencyPolicy)
        setConnPoolPolicy(poolReusePolicy)
        // .setTimeToLive(timeToLive)
        setSchemePortResolver(schemePortResolver)
        setDnsResolver(dnsResolver)

        connFactory?.let { setConnectionFactory(it) }
    }
//    PoolingHttpClientConnectionManager(
//        socketFactoryRegistry,
//        poolConcurrencyPolicy,
//        poolReusePolicy,
//        timeToLive,
//        schemePortResolver,
//        dnsResolver,
//        connFactory
//    )

/**
 * [PoolingHttpClientConnectionManager] 를 생성합니다.
 */
fun poolingHttpClientConnectionManagerOf(
    // sslSocketFactory: LayeredConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory(),
    poolConcurrencyPolicy: PoolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT,
    poolReusePolicy: PoolReusePolicy = PoolReusePolicy.LIFO,
    schemePortResolver: SchemePortResolver? = null,
    dnsResolver: DnsResolver? = null,
    connFactory: HttpConnectionFactory<ManagedHttpClientConnection>? = null,
    maxConnTotal: Int? = null,
    maxConnPerRoute: Int? = null,
    connectionConfig: ConnectionConfig = ConnectionConfig.DEFAULT,
    socketConfig: SocketConfig = SocketConfig.DEFAULT,
    socketConfigResolver: Resolver<HttpRoute, SocketConfig>? = null,
    initializer: PoolingHttpClientConnectionManagerBuilder.() -> Unit = {},
): PoolingHttpClientConnectionManager =
    poolingHttpClientConnectionManager {
//        setSSLSocketFactory(sslSocketFactory)
        setPoolConcurrencyPolicy(poolConcurrencyPolicy)
        setConnPoolPolicy(poolReusePolicy)
        setSchemePortResolver(schemePortResolver)
        setDnsResolver(dnsResolver)
        setConnectionFactory(connFactory)

        maxConnTotal?.let { setMaxConnTotal(it) }
        maxConnPerRoute?.let { setMaxConnPerRoute(it) }
        socketConfigResolver?.let { setSocketConfigResolver(it) }

        setDefaultConnectionConfig(connectionConfig)
        setDefaultSocketConfig(socketConfig)

        initializer()
    }
