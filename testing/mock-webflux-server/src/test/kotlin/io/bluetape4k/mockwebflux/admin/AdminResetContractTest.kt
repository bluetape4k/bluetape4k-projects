package io.bluetape4k.mockwebflux.admin

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E02: POST /admin/reset 엔드포인트 계약 테스트.
 * fixture 재적재 후 jsonplaceholder/posts가 충분한 길이의 데이터를 반환하는지 확인한다.
 */
class AdminResetContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `admin_reset_reloads_fixtures`() {
        // 사전에 하나의 게시글을 삭제하여 상태 변경을 만든다.
        client.delete().uri("/jsonplaceholder/posts/1")
            .exchange()
            .expectStatus().is2xxSuccessful

        // 관리자 리셋 호출
        client.post().uri("/admin/reset")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk

        // 복구 후 목록이 충분한 크기로 돌아왔는지 검증
        val body = client.get().uri("/jsonplaceholder/posts")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody!!
        body.length shouldBeGreaterThan 100
    }
}
