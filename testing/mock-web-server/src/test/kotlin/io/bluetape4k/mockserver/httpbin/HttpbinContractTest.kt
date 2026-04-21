package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockserver.MockServerApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * httpbin 엔드포인트에 대한 계약 테스트.
 *
 * MockServerApplication을 MockMvc로 구동하여 각 httpbin 엔드포인트의 HTTP 응답 상태 코드 및
 * 응답 바디 필드를 검증한다.
 */
@SpringBootTest(classes = [MockServerApplication::class])
class HttpbinContractTest {

    companion object : KLogging()

    @Autowired
    private lateinit var ctx: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build()
    }

    /**
     * GET /httpbin/get → 200, JSON body에 url 필드 존재
     */
    @Test
    fun `GET httpbin get returns 200 with url field`() {
        mockMvc.perform(get("/httpbin/get"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").exists())
            .andDo { log.info { "GET /httpbin/get → 200" } }
    }

    /**
     * POST /httpbin/post → 200
     */
    @Test
    fun `POST httpbin post returns 200`() {
        mockMvc.perform(
            post("/httpbin/post")
                .contentType(MediaType.TEXT_PLAIN)
                .content("test body")
        )
            .andExpect(status().isOk)
            .andDo { log.info { "POST /httpbin/post → 200" } }
    }

    /**
     * GET /httpbin/status/200 → 200
     */
    @Test
    fun `GET httpbin status 200 returns 200`() {
        mockMvc.perform(get("/httpbin/status/200"))
            .andExpect(status().isOk)
    }

    /**
     * GET /httpbin/status/404 → 404
     */
    @Test
    fun `GET httpbin status 404 returns 404`() {
        mockMvc.perform(get("/httpbin/status/404"))
            .andExpect(status().isNotFound)
    }

    /**
     * GET /httpbin/delay/1 → 200 (1초 이상 응답 지연)
     */
    @Test
    fun `GET httpbin delay 1 returns 200 after delay`() {
        val start = System.currentTimeMillis()
        mockMvc.perform(get("/httpbin/delay/1"))
            .andExpect(status().isOk)
        val elapsed = System.currentTimeMillis() - start
        log.info { "GET /httpbin/delay/1 elapsed=${elapsed}ms" }
        assert(elapsed >= 1000L) { "Expected delay >= 1000ms, but got ${elapsed}ms" }
    }

    /**
     * GET /httpbin/headers → 200, JSON에 headers 필드 존재
     */
    @Test
    fun `GET httpbin headers returns 200 with headers field`() {
        mockMvc.perform(get("/httpbin/headers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.headers").exists())
    }

    /**
     * GET /httpbin/ip → 200, JSON에 origin 필드 존재
     */
    @Test
    fun `GET httpbin ip returns 200 with origin field`() {
        mockMvc.perform(get("/httpbin/ip"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.origin").exists())
    }

    /**
     * GET /httpbin/uuid → 200, JSON에 uuid 필드 존재
     */
    @Test
    fun `GET httpbin uuid returns 200 with uuid field`() {
        mockMvc.perform(get("/httpbin/uuid"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uuid").exists())
    }

    /**
     * GET /httpbin/gzip → 200 (압축 응답)
     */
    @Test
    fun `GET httpbin gzip returns 200`() {
        mockMvc.perform(get("/httpbin/gzip"))
            .andExpect(status().isOk)
    }

    /**
     * GET /httpbin/image/png → 200
     */
    @Test
    fun `GET httpbin image png returns 200`() {
        mockMvc.perform(get("/httpbin/image/png"))
            .andExpect(status().isOk)
    }
}
