package io.bluetape4k.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.ElasticsearchServer
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Elasticsearch 테스트에서 공통으로 사용하는 유틸리티 모음.
 *
 * ## 임시 인덱스 생성/삭제
 * ```kotlin
 * val indexName = ElasticsearchTestFixtures.randomIndexName()
 * asyncClient.createTestIndex(indexName).await()
 * try {
 *     // ... 테스트 로직
 * } finally {
 *     asyncClient.deleteTestIndex(indexName).await()
 * }
 * ```
 *
 * ## 테스트 AsyncClient 빌드
 * ```kotlin
 * val client = ElasticsearchTestFixtures.asyncClientOf(server)
 * ```
 */
object ElasticsearchTestFixtures : KLogging() {

    /**
     * 테스트용 임시 인덱스 이름을 생성합니다.
     *
     * 반환값은 `test-<uuid>` 형식이며, Elasticsearch 인덱스 이름 규칙(소문자)을 따릅니다.
     *
     * @param prefix 인덱스 이름 접두사 (기본값: `"test"`)
     * @return 임시 인덱스 이름
     */
    fun randomIndexName(prefix: String = "test"): String =
        "$prefix-${UUID.randomUUID().toString().lowercase()}"

    /**
     * [ElasticsearchServer] 로부터 SSL + Basic Auth 가 적용된 [ElasticsearchAsyncClient] 를 생성합니다.
     *
     * ```kotlin
     * val client = ElasticsearchTestFixtures.asyncClientOf(server)
     * ```
     *
     * @param server 실행 중인 [ElasticsearchServer] 인스턴스
     * @param username Basic Auth 사용자명 (기본값: `"elastic"`)
     * @return [ElasticsearchAsyncClient] 인스턴스
     */
    fun asyncClientOf(
        server: ElasticsearchServer,
        username: String = ElasticsearchDefaults.DEFAULT_USERNAME,
    ): ElasticsearchAsyncClient =
        ElasticsearchClients.asyncClientOf(
            host = server.host,
            port = server.getMappedPort(ElasticsearchServer.PORT),
            scheme = "https",
            username = username,
            password = server.password,
            sslContext = server.createSslContextFromCa(),
        )

    /**
     * 테스트용 인덱스를 생성합니다.
     *
     * ```kotlin
     * val indexName = ElasticsearchTestFixtures.randomIndexName()
     * asyncClient.createTestIndex(indexName).await()
     * ```
     *
     * @param indexName 생성할 인덱스 이름
     * @return [CreateIndexResponse] 를 담은 [CompletableFuture]
     */
    fun ElasticsearchAsyncClient.createTestIndex(indexName: String): CompletableFuture<CreateIndexResponse> =
        indices().create { it.index(indexName) }

    /**
     * 테스트용 인덱스를 삭제합니다.
     *
     * ```kotlin
     * asyncClient.deleteTestIndex(indexName).await()
     * ```
     *
     * @param indexName 삭제할 인덱스 이름
     * @return [DeleteIndexResponse] 를 담은 [CompletableFuture]
     */
    fun ElasticsearchAsyncClient.deleteTestIndex(indexName: String): CompletableFuture<DeleteIndexResponse> =
        indices().delete { it.index(indexName) }
}
