package io.bluetape4k.mockserver.admin

import io.bluetape4k.mockserver.MockServerTestBase
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * E01: GET /ping 엔드포인트 계약 테스트.
 *
 * Testcontainers의 HttpWaitStrategy가 서버 생존 여부를 확인할 때 사용하는 엔드포인트를 검증한다.
 */
class PingContractTest: MockServerTestBase() {

    /** E01: GET /ping → 200 / body "pong" */
    @Test
    fun `ping_returns_pong`() {
        val req = Request.Builder().url("$baseUrl/ping").get().build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.body!!.string() shouldBeEqualTo "pong"
        }
    }
}
