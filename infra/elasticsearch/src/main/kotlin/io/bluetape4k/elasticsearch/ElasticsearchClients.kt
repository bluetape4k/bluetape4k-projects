package io.bluetape4k.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.JsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.ElasticsearchTransportConfig
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.util.Timeout
import java.util.Base64
import javax.net.ssl.SSLContext

/**
 * `co.elastic.clients` 기반 Elasticsearch 클라이언트 팩토리입니다.
 *
 * ## 개요
 * Lettuce/Cassandra 패턴(singleton object + factory methods)을 따릅니다.
 * 로깅은 factory 가 일반 fun 만 가지므로 `KLogging()` 을 사용합니다 (suspend 호출 없음).
 *
 * ES 9.x 는 `Rest5ClientTransport` (HC5 기반) 를 기본 transport 로 사용합니다.
 * (`org.elasticsearch.client.RestClient` 기반의 레거시 `RestClientTransport` 와 다름)
 *
 * ## 사용 예시
 * ```kotlin
 * // 기본 연결 (HTTP)
 * val client = ElasticsearchClients.asyncClientOf(host = "localhost", port = 9200, scheme = "http")
 *
 * // 인증 + SSL (HTTPS)
 * val client = ElasticsearchClients.asyncClientOf(
 *     host = "my-es.example.com",
 *     port = 9200,
 *     scheme = "https",
 *     username = "elastic",
 *     password = "secret",
 *     sslContext = mySslContext,
 * )
 * ```
 *
 * ## Virtual Thread 안전성
 * `synchronized` / `@Synchronized` 를 사용하지 않으며, 필요한 경우 `ReentrantLock` 을 사용합니다.
 */
object ElasticsearchClients : KLogging() {

    /** Testcontainers 및 JVM cold-start 환경에서 연결 handshake를 허용하는 시간입니다. */
    private const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Long = 10_000L

    /** 기본 Elasticsearch 호스트 */
    const val DEFAULT_HOST: String = ElasticsearchDefaults.DEFAULT_HOST

    /** 기본 Elasticsearch 포트 */
    const val DEFAULT_PORT: Int = ElasticsearchDefaults.DEFAULT_PORT

    /** 기본 연결 스킴 */
    const val DEFAULT_SCHEME: String = ElasticsearchDefaults.DEFAULT_SCHEME

    /** 기본 사용자명 */
    const val DEFAULT_USERNAME: String = ElasticsearchDefaults.DEFAULT_USERNAME

    /**
     * [ElasticsearchAsyncClient] 를 생성합니다.
     *
     * `mapper` 기본값은 `null` — `null` 이면 `ElasticsearchTransportConfig.Builder` 가
     * 자동으로 `JacksonJsonpMapper` 를 선택합니다. 명시적 mapper 를 전달하면 해당 mapper 를 사용합니다.
     *
     * ## 사용 예시
     * ```kotlin
     * val client = ElasticsearchClients.asyncClientOf(
     *     host = "localhost",
     *     port = 9200,
     *     scheme = "https",
     *     username = "elastic",
     *     password = "secret",
     * )
     * // 사용 후 close()를 호출하거나 use { } 블록을 사용하세요.
     * client.use { c ->
     *     c.ping()
     * }
     * ```
     *
     * @param host       연결할 Elasticsearch 호스트 (기본값: [DEFAULT_HOST])
     * @param port       연결할 Elasticsearch 포트 (기본값: [DEFAULT_PORT])
     * @param scheme     연결 스킴 — `"https"` 또는 `"http"` (기본값: [DEFAULT_SCHEME])
     * @param username   Basic Auth 사용자명 (null 이면 인증 없음)
     * @param password   Basic Auth 비밀번호 (null 이면 인증 없음)
     * @param sslContext SSL 컨텍스트 (null 이면 기본값 사용)
     * @param mapper     JSON 직렬화에 사용할 [JsonpMapper] (null 이면 JacksonJsonpMapper 자동 사용)
     * @return [ElasticsearchAsyncClient] 인스턴스
     */
    @JvmStatic
    @JvmOverloads
    fun asyncClientOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper? = null,
    ): ElasticsearchAsyncClient {
        host.requireNotBlank("host")
        port.requirePositiveNumber("port")
        scheme.requireNotBlank("scheme")

        val transport = transportOf(host, port, scheme, username, password, sslContext, mapper)
        return ElasticsearchAsyncClient(transport)
    }

    /**
     * [Rest5Client] 를 직접 주입하여 [ElasticsearchAsyncClient] 를 생성합니다.
     *
     * 고급 사용 사례(커스텀 HTTP 설정, 프록시 등)를 위한 오버로드입니다.
     *
     * ```kotlin
     * val rest5Client = Rest5Client.builder(HttpHost("https", "localhost", 9200)).build()
     * val client = ElasticsearchClients.asyncClientOf(rest5Client)
     * ```
     *
     * @param rest5Client 미리 구성된 [Rest5Client]
     * @param mapper      JSON 직렬화에 사용할 [JsonpMapper] (null 이면 JacksonJsonpMapper 자동 사용)
     * @return [ElasticsearchAsyncClient] 인스턴스
     */
    @JvmStatic
    @JvmOverloads
    fun asyncClientOf(
        rest5Client: Rest5Client,
        mapper: JsonpMapper = co.elastic.clients.json.jackson.JacksonJsonpMapper(),
    ): ElasticsearchAsyncClient {
        val transport = Rest5ClientTransport(rest5Client, mapper)
        return ElasticsearchAsyncClient(transport)
    }

    /**
     * sync [ElasticsearchClient] 를 생성합니다.
     *
     * **주의**: 이 클라이언트는 I/O 호출 시 호출 스레드를 블로킹합니다.
     * Virtual Thread 환경에서는 사용 가능하지만, Coroutines 환경에서는
     * [asyncClientOf] 를 사용하는 것을 강력히 권장합니다.
     *
     * ```kotlin
     * val client = ElasticsearchClients.clientOf(
     *     host = "localhost",
     *     port = 9200,
     *     scheme = "https",
     * )
     * // 사용 후 반드시 close()를 호출하세요.
     * client.use { c ->
     *     c.ping()
     * }
     * ```
     *
     * @param host       연결할 Elasticsearch 호스트 (기본값: [DEFAULT_HOST])
     * @param port       연결할 Elasticsearch 포트 (기본값: [DEFAULT_PORT])
     * @param scheme     연결 스킴 — `"https"` 또는 `"http"` (기본값: [DEFAULT_SCHEME])
     * @param username   Basic Auth 사용자명 (null 이면 인증 없음)
     * @param password   Basic Auth 비밀번호 (null 이면 인증 없음)
     * @param sslContext SSL 컨텍스트 (null 이면 기본값 사용)
     * @param mapper     JSON 직렬화에 사용할 [JsonpMapper] (null 이면 JacksonJsonpMapper 자동 사용)
     * @return [ElasticsearchClient] 인스턴스
     */
    @JvmStatic
    @JvmOverloads
    fun clientOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper? = null,
    ): ElasticsearchClient {
        host.requireNotBlank("host")
        port.requirePositiveNumber("port")
        scheme.requireNotBlank("scheme")

        val transport = transportOf(host, port, scheme, username, password, sslContext, mapper)
        return ElasticsearchClient(transport)
    }

    /**
     * 공통 [ElasticsearchTransport] 를 빌드합니다.
     *
     * ES 9.x 에서는 `Rest5ClientTransport` (HC5 기반) 를 기본으로 사용합니다.
     * `ElasticsearchTransportConfig.Builder` 를 통해 자동 설정이 이루어집니다.
     *
     * HC5 [HttpHost] 생성자 순서는 `(scheme, host, port)` 입니다 (HC4 와 순서가 다름).
     *
     * ## 내부 빌드 흐름
     * ```
     * ElasticsearchTransportConfig.Builder()
     *     .host("${scheme}://${host}:${port}")
     *     .usernameAndPassword(username, password)   // 인증 있을 때만
     *     .sslContext(sslContext)                     // SSL 있을 때만
     *     .jsonMapper(mapper)                         // mapper 있을 때만
     *     .build()
     *     .buildTransport()   // 기본 Rest5ClientTransport 생성
     *
     * 기본 Rest5Client의 연결 timeout은 1초이므로, Testcontainers 및 JVM cold-start
     * 환경을 위해 low-level Rest5Client를 직접 구성하면서 나머지 transport 설정은
     * 그대로 전달합니다.
     * ```
     *
     * @param host       연결할 Elasticsearch 호스트
     * @param port       연결할 Elasticsearch 포트
     * @param scheme     연결 스킴
     * @param username   Basic Auth 사용자명 (null 이면 인증 없음)
     * @param password   Basic Auth 비밀번호 (null 이면 인증 없음)
     * @param sslContext SSL 컨텍스트 (null 이면 기본값 사용)
     * @param mapper     JSON 직렬화에 사용할 [JsonpMapper] (null 이면 JacksonJsonpMapper 자동 사용)
     * @return [ElasticsearchTransport] 인스턴스
     */
    @JvmStatic
    @JvmOverloads
    fun transportOf(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        scheme: String = DEFAULT_SCHEME,
        username: String? = null,
        password: String? = null,
        sslContext: SSLContext? = null,
        mapper: JsonpMapper? = null,
    ): ElasticsearchTransport {
        host.requireNotBlank("host")
        port.requirePositiveNumber("port")
        scheme.requireNotBlank("scheme")

        val config = ElasticsearchTransportConfig.Builder()
            .host("$scheme://$host:$port")
            .apply {
                if (username != null && password != null) {
                    usernameAndPassword(username, password)
                }
            }
            .apply {
                if (sslContext != null) {
                    sslContext(sslContext)
                }
            }
            .apply {
                if (mapper != null) {
                    jsonMapper(mapper)
                }
            }
            .build()

        return Rest5ClientTransport(
            Rest5Client.builder(config.hosts())
                .apply {
                    val username = config.username()
                    val password = config.password()
                    if (username != null && password != null) {
                        val credentials = Base64.getEncoder()
                            .encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
                        setDefaultHeaders(
                            arrayOf<Header>(BasicHeader("Authorization", "Basic $credentials"))
                        )
                    }
                    config.sslContext()?.let(::setSSLContext)
                    setCompressionEnabled(config.useCompression())
                    setConnectionConfigCallback { connectionConfig: ConnectionConfig.Builder ->
                        connectionConfig.setConnectTimeout(Timeout.ofMilliseconds(DEFAULT_CONNECT_TIMEOUT_MILLIS))
                    }
                }
                .build(),
            config.mapper() ?: co.elastic.clients.json.jackson.JacksonJsonpMapper(),
            Rest5ClientOptions.of(config.transportOptions()),
            config.instrumentation(),
        )
    }
}
