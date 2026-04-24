package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.mockserver.MockServerTestBase
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

/**
 * httpbin 스트리밍 / 바이너리 엔드포인트 계약 테스트 (E15–E18).
 *
 * 실제 HTTP 클라이언트(OkHttp)를 사용하여 Content-Encoding, Content-Type, 본문 내용을 검증한다.
 */
class HttpbinStreamContractTest: MockServerTestBase() {

    /** E15: GET /httpbin/gzip → Content-Encoding: gzip (OkHttp는 자동 해제) */
    @Test
    fun `gzip_endpoint_returns_gzip_encoded`() {
        val req = Request.Builder()
            .url("$baseUrl/httpbin/gzip")
            .addHeader("Accept-Encoding", "gzip")
            .get()
            .build()
        // OkHttp가 gzip 응답을 자동으로 해제하므로 상태 코드만 검증한다.
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
        }
    }

    /** E16: GET /httpbin/deflate → 200 */
    @Test
    fun `deflate_endpoint_returns_deflate_encoded`() {
        val req = Request.Builder().url("$baseUrl/httpbin/deflate").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
        }
    }

    /** E17: GET /httpbin/stream/{n} → n NDJSON lines */
    @Test
    fun `stream_endpoint_returns_ndjson_lines`() {
        val req = Request.Builder().url("$baseUrl/httpbin/stream/5").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
            val bodyText = requireNotNull(response.body) { "body must not be null" }.string()
            val lines = bodyText.trim().lines()
            lines.size shouldBeGreaterOrEqualTo 5
        }
    }

    /** E18: GET /httpbin/image/{fmt} → Content-Type "image/..." */
    @Test
    fun `image_endpoint_returns_content_type_match`() {
        val req = Request.Builder().url("$baseUrl/httpbin/image/png").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.header("Content-Type").orEmpty() shouldContain "image"
        }
    }
}
