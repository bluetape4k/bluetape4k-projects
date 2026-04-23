package io.bluetape4k.mockserver.web

import io.bluetape4k.mockserver.MockServerTestBase
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * web 콘텐츠 엔드포인트 계약 테스트 (E35–E36).
 *
 * 목업 HTML 페이지를 반환하는 엔드포인트의 Content-Type과 본문을 검증한다.
 */
class WebContentContractTest: MockServerTestBase() {

    /** E35: GET /web/random → text/html 응답 */
    @Test
    fun `web random returns html`() {
        val req = Request.Builder().url("$baseUrl/web/random").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.header("Content-Type").orEmpty() shouldContain "text/html"
        }
    }

    /** E36: GET /web/{name} → 각 이름에 대해 HTML 반환 */
    @ParameterizedTest
    @ValueSource(strings = ["home", "naver", "google", "login", "article"])
    fun `web named returns html for each name`(name: String) {
        val req = Request.Builder().url("$baseUrl/web/$name").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.body.string().lowercase() shouldContain "<html"
        }
    }
}
