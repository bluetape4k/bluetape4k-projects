package io.bluetape4k.testcontainers.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import okhttp3.OkHttpClient
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory

/**
 * [BluetapeHttpServer] 통합 테스트.
 *
 * Docker가 없는 환경에서는 [BeforeAll]에서 `assumeTrue`로 테스트를 건너뜁니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BluetapeHttpServerTest {

    companion object : KLogging() {
        private val server: BluetapeHttpServer by lazy {
            BluetapeHttpServer().apply { start() }
        }
    }

    private val httpClient = OkHttpClient()

    @BeforeAll
    fun checkDocker() {
        assumeTrue(
            DockerClientFactory.instance().isDockerAvailable,
            "Docker is not available — skipping BluetapeHttpServerTest"
        )
        // lazy 초기화 트리거 (Docker 가용성 확인 후 시작)
        log.info { "BluetapeHttpServer url=${server.url}" }
    }

    /**
     * `/ping` 엔드포인트가 HTTP 200과 body "pong"을 반환하는지 확인합니다.
     */
    @Test
    fun `ping endpoint returns 200 and pong`() {
        val request = Request.Builder()
            .url("${server.url}/ping")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.body.shouldNotBeNull()
            response.body!!.string() shouldBeEqualTo "pong"
        }
    }

    /**
     * httpbin `/get` 엔드포인트가 HTTP 200과 `url` 필드를 포함하는 JSON을 반환하는지 확인합니다.
     */
    @Test
    fun `httpbin get endpoint returns 200 with url field`() {
        val request = Request.Builder()
            .url("${server.httpbinUrl}/get")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
            val body = response.body.shouldNotBeNull()
            val bodyString = body.string()
            bodyString shouldContain "url"
        }
    }

    /**
     * httpbin `/status/418` 엔드포인트가 HTTP 418을 반환하는지 확인합니다.
     */
    @Test
    fun `httpbin status 418 returns 418`() {
        val request = Request.Builder()
            .url("${server.httpbinUrl}/status/418")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 418
        }
    }

    /**
     * jsonplaceholder `/posts` 엔드포인트가 HTTP 200과 비어있지 않은 JSON 배열을 반환하는지 확인합니다.
     */
    @Test
    fun `jsonplaceholder posts returns 200 with non-empty array`() {
        val request = Request.Builder()
            .url("${server.jsonplaceholderUrl}/posts")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
            val body = response.body.shouldNotBeNull()
            val bodyString = body.string()
            bodyString.shouldNotBeEmpty()
        }
    }

    /**
     * jsonplaceholder `/posts/1` 엔드포인트가 HTTP 200을 반환하는지 확인합니다.
     */
    @Test
    fun `jsonplaceholder posts 1 returns 200`() {
        val request = Request.Builder()
            .url("${server.jsonplaceholderUrl}/posts/1")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
        }
    }

    /**
     * web `/random` 엔드포인트가 HTTP 200과 `text/html` Content-Type을 반환하는지 확인합니다.
     */
    @Test
    fun `web random returns 200 with html content type`() {
        val request = Request.Builder()
            .url("${server.webUrl}/random")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
            val contentType = response.header("Content-Type").shouldNotBeNull()
            contentType shouldContain "text/html"
        }
    }

    /**
     * web `/naver` 엔드포인트가 HTTP 200을 반환하는지 확인합니다.
     */
    @Test
    fun `web naver returns 200`() {
        val request = Request.Builder()
            .url("${server.webUrl}/naver")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            response.code shouldBeEqualTo 200
        }
    }
}
