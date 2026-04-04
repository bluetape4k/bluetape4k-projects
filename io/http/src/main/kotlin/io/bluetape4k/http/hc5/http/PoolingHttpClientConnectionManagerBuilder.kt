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
 * ```kotlin
 * val cm = poolingHttpClientConnectionManager {
 *     setMaxConnPerRoute(5)
 *     setMaxConnTotal(5)
 * }
 * ```
 */
inline fun poolingHttpClientConnectionManager(
    builder: PoolingHttpClientConnectionManagerBuilder.() -> Unit,
): PoolingHttpClientConnectionManager =
    PoolingHttpClientConnectionManagerBuilder.create().apply(builder).build()

/**
 * [PoolingHttpClientConnectionManager] 를 생성합니다.
 *
 * ```kotlin
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
 *
 * ```kotlin
 * val cm = poolingHttpClientConnectionManagerOf(
 *     poolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT,
 *     poolReusePolicy = PoolReusePolicy.LIFO,
 *     timeToLive = TimeValue.ofMinutes(5),
 * )
 * val client = httpClientOf(cm)
 * ```
 *
 * @param poolConcurrencyPolicy 연결 풀 동시성 정책
 * @param poolReusePolicy 연결 풀 재사용 정책
 * @param timeToLive 연결 생존 시간
 * @param schemePortResolver 스킴별 포트 해석기
 * @param dnsResolver DNS 해석기
 * @param connFactory 연결 팩토리
 * @return [PoolingHttpClientConnectionManager] 인스턴스
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
 *
 * ```kotlin
 * val cm = poolingHttpClientConnectionManagerOf(
 *     maxConnTotal = 100,
 *     maxConnPerRoute = 10,
 *     connectionConfig = connectionConfig {
 *         setConnectTimeout(Timeout.ofSeconds(10))
 *     },
 * )
 * val client = httpClientOf(cm)
 * ```
 *
 * @param poolConcurrencyPolicy 연결 풀 동시성 정책
 * @param poolReusePolicy 연결 풀 재사용 정책
 * @param schemePortResolver 스킴별 포트 해석기
 * @param dnsResolver DNS 해석기
 * @param connFactory 연결 팩토리
 * @param maxConnTotal 최대 전체 연결 수
 * @param maxConnPerRoute 라우트당 최대 연결 수
 * @param connectionConfig 연결 설정
 * @param socketConfig 소켓 설정
 * @param socketConfigResolver 소켓 설정 해석기
 * @param builder [PoolingHttpClientConnectionManagerBuilder] 추가 설정 블록
 * @return [PoolingHttpClientConnectionManager] 인스턴스
 */
inline fun poolingHttpClientConnectionManagerOf(
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
    builder: PoolingHttpClientConnectionManagerBuilder.() -> Unit = {},
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

        builder()
    }
