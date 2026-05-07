package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockserver.MockServerApplication
import jakarta.servlet.http.Cookie
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*

/**
 * httpbin 고급 엔드포인트(인증, 쿠키, 리다이렉트, 캐시, ETag)에 대한 계약 테스트.
 */
@SpringBootTest(classes = [MockServerApplication::class])
class HttpbinAdvancedContractTest {

    companion object : KLogging()

    @Autowired
    private lateinit var ctx: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build()
    }

    @Test
    fun `GET httpbin redirect 3 returns 302 with location`() {
        mockMvc.perform(get("/httpbin/redirect/3"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/redirect/2"))
            .andDo { log.info { "GET /httpbin/redirect/3 → 302" } }
    }

    @Test
    fun `GET httpbin redirect 0 redirects to get`() {
        mockMvc.perform(get("/httpbin/redirect/0"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/get"))
    }

    @Test
    fun `GET httpbin cookies returns empty cookies when no cookie sent`() {
        mockMvc.perform(get("/httpbin/cookies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cookies").isMap)
    }

    @Test
    fun `GET httpbin cookies returns sent cookies`() {
        mockMvc.perform(
            get("/httpbin/cookies")
                .cookie(Cookie("foo", "bar"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cookies.foo").value("bar"))
    }

    @Test
    fun `GET httpbin cookies set redirects and sets cookies`() {
        mockMvc.perform(get("/httpbin/cookies/set").param("name", "value"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/cookies"))
    }

    @Test
    fun `GET httpbin cookies delete redirects and deletes cookies`() {
        mockMvc.perform(get("/httpbin/cookies/delete").param("name", ""))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/cookies"))
    }

    @Test
    fun `GET httpbin basic-auth with valid credentials returns authenticated`() {
        val credentials = Base64.getEncoder().encodeToString("testuser:testpass".toByteArray())
        mockMvc.perform(
            get("/httpbin/basic-auth/testuser/testpass")
                .header("Authorization", "Basic $credentials")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.user").value("testuser"))
    }

    @Test
    fun `GET httpbin basic-auth with wrong credentials returns 401`() {
        val credentials = Base64.getEncoder().encodeToString("wrong:creds".toByteArray())
        mockMvc.perform(
            get("/httpbin/basic-auth/testuser/testpass")
                .header("Authorization", "Basic $credentials")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET httpbin basic-auth without authorization header returns 401`() {
        mockMvc.perform(get("/httpbin/basic-auth/testuser/testpass"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", "Basic realm=\"Fake Realm\""))
    }

    @Test
    fun `GET httpbin basic-auth with invalid base64 returns 401`() {
        mockMvc.perform(
            get("/httpbin/basic-auth/testuser/testpass")
                .header("Authorization", "Basic !!!invalid!!!")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET httpbin bearer with valid token returns authenticated`() {
        mockMvc.perform(
            get("/httpbin/bearer")
                .header("Authorization", "Bearer my-secret-token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.token").value("my-secret-token"))
    }

    @Test
    fun `GET httpbin bearer without authorization returns 401`() {
        mockMvc.perform(get("/httpbin/bearer"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET httpbin bearer with non-bearer token returns 401`() {
        mockMvc.perform(
            get("/httpbin/bearer")
                .header("Authorization", "Basic something")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET httpbin cache without If-Modified-Since returns 200 with Last-Modified`() {
        mockMvc.perform(get("/httpbin/cache"))
            .andExpect(status().isOk)
            .andExpect(header().exists("Last-Modified"))
            .andExpect(jsonPath("$.cached").value(false))
    }

    @Test
    fun `GET httpbin cache with If-Modified-Since returns 304`() {
        mockMvc.perform(
            get("/httpbin/cache")
                .header("If-Modified-Since", "Wed, 01 Jan 2025 00:00:00 GMT")
        )
            .andExpect(status().isNotModified)
    }

    @Test
    fun `GET httpbin cache value sets Cache-Control header`() {
        mockMvc.perform(get("/httpbin/cache/300"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "public, max-age=300"))
            .andExpect(jsonPath("$.max-age").value(300))
    }

    @Test
    fun `GET httpbin etag with matching If-None-Match returns 304`() {
        mockMvc.perform(
            get("/httpbin/etag/abc123")
                .header("If-None-Match", "\"abc123\"")
        )
            .andExpect(status().isNotModified)
    }

    @Test
    fun `GET httpbin etag without If-None-Match returns 200 with ETag`() {
        mockMvc.perform(get("/httpbin/etag/abc123"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ETAG, "\"abc123\""))
            .andExpect(jsonPath("$.etag").value("abc123"))
    }

    @Test
    fun `GET httpbin bytes returns correct number of bytes`() {
        val result = mockMvc.perform(get("/httpbin/bytes/64"))
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsByteArray
        body.shouldNotBeNull()
        body.size shouldBeEqualTo 64
    }

    @Test
    fun `GET httpbin bytes with invalid n returns 400`() {
        mockMvc.perform(get("/httpbin/bytes/0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET httpbin user-agent returns user-agent field`() {
        mockMvc.perform(
            get("/httpbin/user-agent")
                .header("User-Agent", "TestAgent/1.0")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user-agent").value("TestAgent/1.0"))
    }

    @Test
    fun `GET httpbin anything returns request info`() {
        mockMvc.perform(get("/httpbin/anything/some/path"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").exists())
            .andExpect(jsonPath("$.headers").exists())
    }

    @Test
    fun `GET httpbin deflate returns 200`() {
        mockMvc.perform(get("/httpbin/deflate"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET httpbin stream returns ndjson`() {
        val result = mockMvc.perform(get("/httpbin/stream/3"))
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsString
        val lines = body.trim().lines()
        lines.size shouldBeEqualTo 3
        lines.forEach { line ->
            line.contains("\"id\"").shouldBeTrue()
        }
    }

    @Test
    fun `GET httpbin stream with invalid n returns 400`() {
        mockMvc.perform(get("/httpbin/stream/0"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET httpbin image with unsupported format returns 400`() {
        mockMvc.perform(get("/httpbin/image/bmp"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET httpbin image jpeg returns 200`() {
        mockMvc.perform(get("/httpbin/image/jpeg"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET httpbin image svg returns 200 with svg content type`() {
        val result = mockMvc.perform(get("/httpbin/image/svg"))
            .andExpect(status().isOk)
            .andReturn()

        result.response.getHeader("Content-Type") shouldBeEqualTo "image/svg+xml"
    }

    @Test
    fun `GET httpbin delay with 0 seconds returns 200`() {
        mockMvc.perform(get("/httpbin/delay/0"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET httpbin delay with decimal seconds returns 200`() {
        // 0.1초(100ms) 지연 — 소수점 지원 확인
        mockMvc.perform(get("/httpbin/delay/0.1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET httpbin delay with invalid seconds returns 400`() {
        mockMvc.perform(get("/httpbin/delay/11"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET httpbin delay with invalid decimal seconds returns 400`() {
        mockMvc.perform(get("/httpbin/delay/10.1"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET httpbin status with invalid code returns 400`() {
        mockMvc.perform(get("/httpbin/status/999"))
            .andExpect(status().isBadRequest)
    }

    /** E19: /httpbin/delay/{sec} → 요청 시간만큼 대기 후 200 */
    @Test
    fun `delay_endpoint_waits_requested_seconds`() {
        val start = System.currentTimeMillis()
        mockMvc.perform(get("/httpbin/delay/1"))
            .andExpect(status().isOk)
        val elapsed = System.currentTimeMillis() - start
        require(elapsed >= 1000L) { "Expected delay >= 1000ms, but got ${elapsed}ms" }
    }

    /** E20: /httpbin/redirect/{n} → 302 리다이렉트 체인 */
    @Test
    fun `redirect_endpoint_returns_302_chain`() {
        mockMvc.perform(get("/httpbin/redirect/3"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/redirect/2"))
    }

    /** E21: /httpbin/cookies → 요청 쿠키 리스트 반환 */
    @Test
    fun `cookies_endpoint_lists_cookies`() {
        mockMvc.perform(
            get("/httpbin/cookies")
                .cookie(Cookie("session", "abc"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cookies.session").value("abc"))
    }

    /** E22: /httpbin/cookies/set → 쿠키 저장 후 /httpbin/cookies로 리다이렉트 */
    @Test
    fun `cookies_set_stores_cookie`() {
        mockMvc.perform(get("/httpbin/cookies/set").param("token", "xyz"))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/cookies"))
    }

    /** E23: /httpbin/cookies/delete → 쿠키 제거 후 /httpbin/cookies로 리다이렉트 */
    @Test
    fun `cookies_delete_removes_cookie`() {
        mockMvc.perform(get("/httpbin/cookies/delete").param("token", ""))
            .andExpect(status().isFound)
            .andExpect(header().string("Location", "/httpbin/cookies"))
    }

    /** E24: /httpbin/basic-auth/{u}/{p} → Authorization 미제공 시 401 */
    @Test
    fun `basic_auth_returns_401_on_missing_credentials`() {
        mockMvc.perform(get("/httpbin/basic-auth/user/pass"))
            .andExpect(status().isUnauthorized)
    }

    /** E25: /httpbin/bearer → Bearer 토큰 미제공 시 401 */
    @Test
    fun `bearer_returns_401_without_bearer`() {
        mockMvc.perform(get("/httpbin/bearer"))
            .andExpect(status().isUnauthorized)
    }

    /** E26: /httpbin/cache → If-Modified-Since 헤더 있으면 304 */
    @Test
    fun `cache_returns_304_when_if_modified_since_set`() {
        mockMvc.perform(
            get("/httpbin/cache")
                .header("If-Modified-Since", "Wed, 01 Jan 2025 00:00:00 GMT")
        )
            .andExpect(status().isNotModified)
    }

    /** E27: /httpbin/cache/{seconds} → Cache-Control max-age 설정 */
    @Test
    fun `cache_value_sets_cache_control_max_age`() {
        mockMvc.perform(get("/httpbin/cache/600"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "public, max-age=600"))
    }

    /** E28: /httpbin/etag/{etag} → If-None-Match 일치 시 304 */
    @Test
    fun `etag_returns_304_on_if_none_match`() {
        mockMvc.perform(
            get("/httpbin/etag/contract-etag")
                .header("If-None-Match", "\"contract-etag\"")
        )
            .andExpect(status().isNotModified)
    }
}
