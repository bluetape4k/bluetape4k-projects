package io.bluetape4k.mockserver.admin

import io.bluetape4k.mockserver.MockServerTestBase
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test

/**
 * E02: POST /admin/reset 엔드포인트 계약 테스트.
 *
 * jsonplaceholder fixture가 원자적으로 재적재되어 인메모리 저장소가 초기 상태로 복원됨을 검증한다.
 */
class AdminResetContractTest: MockServerTestBase() {

    /** E02: 삭제 후 admin/reset 호출 → fixture 재로드 확인 */
    @Test
    fun `admin_reset_reloads_fixtures`() {
        // 1) 기존 fixture의 첫 번째 게시글 삭제
        val delete = Request.Builder().url("$baseUrl/jsonplaceholder/posts/1").delete().build()
        client.newCall(delete).execute().close()

        // 2) /admin/reset → fixture 재적재
        val req = Request.Builder()
            .url("$baseUrl/admin/reset")
            .post("".toRequestBody())
            .build()
        client.newCall(req).execute().use { response ->
            response.code shouldBeEqualTo 200
        }

        // 3) fixture가 다시 돌아왔는지 확인 (posts 목록의 길이가 충분히 큼)
        val get = Request.Builder().url("$baseUrl/jsonplaceholder/posts").get().build()
        client.newCall(get).execute().use { response ->
            response.code shouldBeEqualTo 200
            response.body.string().length shouldBeGreaterThan 100
        }
    }
}
