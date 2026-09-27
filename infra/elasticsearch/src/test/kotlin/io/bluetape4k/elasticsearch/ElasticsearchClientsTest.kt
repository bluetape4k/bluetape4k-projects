package io.bluetape4k.elasticsearch

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.storage.ElasticsearchServer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [ElasticsearchClients] 에 대한 테스트.
 *
 * SSL + Basic Auth 연결, DSL 빌더 연결, 인증 실패 시나리오를 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchClientsTest: AbstractElasticsearchTest() {

    companion object: KLogging()

    // --------------------------------------------------------------------------
    // asyncClientOf — SSL + Basic Auth
    // --------------------------------------------------------------------------

    @Test
    fun `asyncClient로 info 조회 성공`() = runTest {
        val response = asyncClient.info().await()
        log.debug { "response=$response" }
        response.shouldNotBeNull()
        response.version().shouldNotBeNull()
        response.version().number().shouldNotBeNull()
    }

    @Test
    fun `asyncClient ping 성공`() = runTest {
        val response = asyncClient.ping().await().shouldNotBeNull()
        log.debug { "response=$response" }
        response.value().shouldBeTrue()
    }

    // --------------------------------------------------------------------------
    // clientOf — 동기 클라이언트
    // --------------------------------------------------------------------------

    @Test
    fun `동기 client로 info 조회 성공`() {
        val response = client.info().shouldNotBeNull()
        log.debug { "response=$response" }
        response.version().shouldNotBeNull()
        response.version().number().shouldNotBeNull()
    }

    @Test
    fun `동기 client ping 성공`() {
        val response = client.ping().shouldNotBeNull()
        log.debug { "response=$response" }
        response.value().shouldBeTrue()
    }

    // --------------------------------------------------------------------------
    // 인증 실패
    // --------------------------------------------------------------------------

    @Test
    fun `잘못된 비밀번호로 연결 시 인증 실패 예외 발생`() = runSuspendIO {
        val wrongClient = ElasticsearchClients.asyncClientOf(
            host = elasticsearch.host,
            port = elasticsearch.getMappedPort(ElasticsearchServer.PORT),
            scheme = "https",
            username = ElasticsearchDefaults.DEFAULT_USERNAME,
            password = "wrong-password",
            sslContext = elasticsearch.createSslContextFromCa(),
        )
        assertFailsWith<Exception> {
            wrongClient.info().await()
        }
    }

    // --------------------------------------------------------------------------
    // DSL builder
    // --------------------------------------------------------------------------

    @Test
    fun `DSL builder로 asyncClient 생성 후 info 조회 성공`() = runSuspendIO {
        val dslClient = elasticsearchAsyncClient {
            host = elasticsearch.host
            port = elasticsearch.getMappedPort(ElasticsearchServer.PORT)
            scheme = "https"
            username = ElasticsearchDefaults.DEFAULT_USERNAME
            password = elasticsearch.password
            sslContext = elasticsearch.createSslContextFromCa()
        }
        val response = dslClient.info().await().shouldNotBeNull()
        log.debug { "response=$response" }
        response.version().shouldNotBeNull()
        response.version().number().shouldNotBeNull()
    }

    @Test
    fun `DSL builder로 동기 client 생성 후 ping 성공`() {
        val dslClient = elasticsearchClient {
            host = elasticsearch.host
            port = elasticsearch.getMappedPort(ElasticsearchServer.PORT)
            scheme = "https"
            username = ElasticsearchDefaults.DEFAULT_USERNAME
            password = elasticsearch.password
            sslContext = elasticsearch.createSslContextFromCa()
        }
        val response = dslClient.ping().shouldNotBeNull()
        log.debug { "response=$response" }
        response.value().shouldBeTrue()
    }
}
