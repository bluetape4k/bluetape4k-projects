package io.bluetape4k.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.JsonpMapper
import javax.net.ssl.SSLContext

/**
 * Elasticsearch 클라이언트 연결 설정 DSL 빌더.
 *
 * [elasticsearchAsyncClient] 및 [elasticsearchClient] DSL 함수와 함께 사용합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * val client = elasticsearchAsyncClient {
 *     host = "localhost"
 *     port = 9200
 *     scheme = "https"
 *     username = "elastic"
 *     password = "changeme"
 * }
 * ```
 *
 * @property host 연결할 Elasticsearch 호스트 (기본값: [ElasticsearchDefaults.DEFAULT_HOST])
 * @property port 연결할 Elasticsearch 포트 (기본값: [ElasticsearchDefaults.DEFAULT_PORT])
 * @property scheme 연결 스킴 — `"https"` 또는 `"http"` (기본값: [ElasticsearchDefaults.DEFAULT_SCHEME])
 * @property username Basic Auth 사용자명 (null 이면 인증 없음)
 * @property password Basic Auth 비밀번호 (null 이면 인증 없음)
 * @property sslContext SSL 컨텍스트 (null 이면 기본값 사용)
 * @property mapper JSON 직렬화에 사용할 [JsonpMapper] (null 이면 JacksonJsonpMapper 자동 감지)
 */
class ElasticsearchClientConfig {
    var host: String = ElasticsearchDefaults.DEFAULT_HOST
    var port: Int = ElasticsearchDefaults.DEFAULT_PORT
    var scheme: String = ElasticsearchDefaults.DEFAULT_SCHEME
    var username: String? = null
    var password: String? = null
    var sslContext: SSLContext? = null
    var mapper: JsonpMapper? = null
}

/**
 * DSL 블록으로 [ElasticsearchAsyncClient] 를 생성합니다.
 *
 * `mapper` 를 지정하지 않으면 클래스패스에 있는 Jackson 버전이 자동 선택됩니다
 * (Jackson 3 우선, Jackson 2 폴백).
 *
 * **사용 예시:**
 * ```kotlin
 * val client = elasticsearchAsyncClient {
 *     host = "localhost"
 *     port = 9200
 *     scheme = "https"
 *     username = "elastic"
 *     password = "changeme"
 * }
 * // 사용 후 close() 를 호출하거나 use { } 블록을 사용하세요.
 * client.use { c ->
 *     c.ping()
 * }
 * ```
 *
 * @param block [ElasticsearchClientConfig] 설정 블록
 * @return 설정이 적용된 [ElasticsearchAsyncClient] 인스턴스
 */
fun elasticsearchAsyncClient(
    block: ElasticsearchClientConfig.() -> Unit,
): ElasticsearchAsyncClient {
    val config = ElasticsearchClientConfig().apply(block)
    return ElasticsearchClients.asyncClientOf(
        host = config.host,
        port = config.port,
        scheme = config.scheme,
        username = config.username,
        password = config.password,
        sslContext = config.sslContext,
        mapper = config.mapper,
    )
}

/**
 * DSL 블록으로 동기 [ElasticsearchClient] 를 생성합니다.
 *
 * **주의**: 이 클라이언트는 I/O 호출 시 호출 스레드를 블로킹합니다.
 * Coroutines 환경에서는 [elasticsearchAsyncClient] 를 사용하는 것을 강력히 권장합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * val client = elasticsearchClient {
 *     host = "localhost"
 *     port = 9200
 *     scheme = "http"
 * }
 * // 사용 후 반드시 close() 를 호출하세요.
 * client.use { c ->
 *     c.ping()
 * }
 * ```
 *
 * @param block [ElasticsearchClientConfig] 설정 블록
 * @return 설정이 적용된 [ElasticsearchClient] 인스턴스
 */
fun elasticsearchClient(
    block: ElasticsearchClientConfig.() -> Unit,
): ElasticsearchClient {
    val config = ElasticsearchClientConfig().apply(block)
    return ElasticsearchClients.clientOf(
        host = config.host,
        port = config.port,
        scheme = config.scheme,
        username = config.username,
        password = config.password,
        sslContext = config.sslContext,
        mapper = config.mapper,
    )
}
