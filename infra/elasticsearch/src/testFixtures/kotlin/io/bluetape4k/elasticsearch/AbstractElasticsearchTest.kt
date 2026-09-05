package io.bluetape4k.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.bluetape4k.elasticsearch.support.jacksonJsonpMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.ElasticsearchServer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * Elasticsearch 통합 테스트를 위한 추상 기반 클래스.
 *
 * [ElasticsearchServer.Launcher.elasticsearch] 싱글턴을 공유하여
 * 모든 하위 테스트 클래스가 동일한 컨테이너를 재사용합니다.
 * 컨테이너 준비는 `@BeforeAll`에서 완료하여 coroutine 테스트 제한시간과 분리합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * class MyElasticsearchTest : AbstractElasticsearchTest() {
 *
 *     @Test
 *     fun `ping should succeed`() = runTest {
 *         val response = asyncClient.ping().await()
 *         response.value().shouldBeTrue()
 *     }
 * }
 * ```
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractElasticsearchTest {

    companion object : KLogging() {

        /** 컨테이너의 준비 대기가 개별 `runTest`의 제한시간을 소모하지 않도록 합니다. */
        @JvmStatic
        @BeforeAll
        fun prepareElasticsearch() {
            elasticsearch
        }

        /**
         * 테스트에서 공유하는 [ElasticsearchServer] 싱글턴 인스턴스.
         * JVM 종료 시 [io.bluetape4k.utils.ShutdownQueue] 를 통해 자동으로 종료됩니다.
         */
        @JvmStatic
        val elasticsearch: ElasticsearchServer by lazy {
            ElasticsearchServer.Launcher.elasticsearch
        }

        /**
         * SSL + Basic Auth 가 적용된 비동기 [ElasticsearchAsyncClient].
         *
         * Coroutines 환경에서 사용하는 것을 권장합니다.
         */
        @JvmStatic
        val asyncClient: ElasticsearchAsyncClient by lazy {
            ElasticsearchClients.asyncClientOf(
                host = elasticsearch.host,
                port = elasticsearch.getMappedPort(ElasticsearchServer.PORT),
                scheme = "https",
                username = ElasticsearchDefaults.DEFAULT_USERNAME,
                password = elasticsearch.password,
                sslContext = elasticsearch.createSslContextFromCa(),
                mapper = jacksonJsonpMapper(),
            )
        }

        /**
         * SSL + Basic Auth 가 적용된 동기 [ElasticsearchClient].
         *
         * Virtual Thread 또는 단순 블로킹 코드에서 사용합니다.
         * Coroutines 환경에서는 [asyncClient] 를 사용하는 것을 권장합니다.
         */
        @JvmStatic
        val client: ElasticsearchClient by lazy {
            ElasticsearchClients.clientOf(
                host = elasticsearch.host,
                port = elasticsearch.getMappedPort(ElasticsearchServer.PORT),
                scheme = "https",
                username = ElasticsearchDefaults.DEFAULT_USERNAME,
                password = elasticsearch.password,
                sslContext = elasticsearch.createSslContextFromCa(),
                mapper = jacksonJsonpMapper(),
            )
        }
    }
}
