package io.bluetape4k.http.hc5.async

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.H2AsyncClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager
import org.apache.hc.core5.http2.config.H2Config

/**
 * 기본 설정으로 [CloseableHttpAsyncClient]를 생성합니다.
 */
@JvmField
val defaultHttpAsyncClient: CloseableHttpAsyncClient =
    HttpAsyncClients.createDefault()

/**
 * [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = httpAsyncClient {
 *    setConnectionManager(cm)
 *    setMaxConnTotal(100)
 *    setMaxConnPerRoute(10)
 *    setDefaultRequestConfig(requestConfig)
 *    setDefaultCredentialsProvider(credentialsProvider)
 * }
 * ```
 *
 * @param builder [HttpAsyncClientBuilder] 설정
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
inline fun httpAsyncClient(
    @BuilderInference builder: HttpAsyncClientBuilder.() -> Unit,
): CloseableHttpAsyncClient {
    return HttpAsyncClients.custom().apply(builder).build().apply { this.start() }
}

/**
 * [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = httpAsyncClientOf(cm) {
 *    setMaxConnTotal(100)
 *    setMaxConnPerRoute(10)
 *    setDefaultRequestConfig(requestConfig)
 *    setDefaultCredentialsProvider(credentialsProvider)
 * }
 * ```
 *
 * @param cm [AsyncClientConnectionManager] 설정
 * @param builder [HttpAsyncClientBuilder] 설정
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
inline fun httpAsyncClientOf(
    cm: AsyncClientConnectionManager = defaultAsyncClientConnectionManager,
    @BuilderInference builder: HttpAsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient = httpAsyncClient {
    setConnectionManager(cm)
    builder()
}

/**
 * 기본 설정과 System Property 설정으로 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = httpAsyncClientSystemOf(cm)
 * ```
 *
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
fun httpAsyncClientSystemOf(): CloseableHttpAsyncClient =
    HttpAsyncClients.createSystem().apply { start() }

/**
 * Http2 기본 설정으로 [CloseableHttpAsyncClient]를 생성합니다.
 */
@JvmField
val defaultH2AsyncClient: CloseableHttpAsyncClient =
    HttpAsyncClients.createHttp2Default().apply { start() }

/**
 * Http2 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = h2AsyncClient {
 *   setH2Config(h2config)
 *   setConnectionManager(cm)
 *   setMaxConnTotal(100)
 *   setMaxConnPerRoute(10)
 * }
 * ```
 *
 * @param builder [H2AsyncClientBuilder] 설정
 * @return [CloseableHttpAsyncClient] 인스턴스
 */
inline fun h2AsyncClient(
    @BuilderInference builder: H2AsyncClientBuilder.() -> Unit,
): CloseableHttpAsyncClient {
    return HttpAsyncClients.customHttp2().apply(builder).build().apply { start() }
}

/**
 * 기본 설정으로 Http2 [CloseableHttpAsyncClient]를 생성합니다.
 */
fun h2AsyncClientOf(): CloseableHttpAsyncClient =
    HttpAsyncClients.createHttp2Default()

/**
 * Http2 [CloseableHttpAsyncClient]를 생성합니다.
 *
 * ```
 * val client = h2AsyncClientOf(h2config) {
 *      setConnectionManager(cm)
 *      setMaxConnTotal(100)
 *      setMaxConnPerRoute(10)
 * }
 * ```
 *
 * @param h2config [H2Config] 설정
 * @param builder [H2AsyncClientBuilder] 설정
 */
inline fun h2AsyncClientOf(
    h2config: H2Config = H2Config.DEFAULT,
    @BuilderInference builder: H2AsyncClientBuilder.() -> Unit = {},
): CloseableHttpAsyncClient {
    return h2AsyncClient {
        setH2Config(h2config)
        builder()
    }
}

/**
 * System Property 설정으로 Http2 [CloseableHttpAsyncClient]를 생성합니다.
 */
fun h2AsyncClientSystemOf(): CloseableHttpAsyncClient =
    HttpAsyncClients.createHttp2System().apply { start() }
