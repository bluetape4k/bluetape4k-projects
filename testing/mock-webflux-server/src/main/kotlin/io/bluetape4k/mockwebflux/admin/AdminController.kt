package io.bluetape4k.mockwebflux.admin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockwebflux.jsonplaceholder.JsonplaceholderService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * mock-webflux-server 관리용 엔드포인트 (WebFlux suspend).
 *
 * fixture 재적재, 서버 상태 확인 등 운영 관리 기능을 제공한다.
 *
 * 모든 관리 엔드포인트는 `X-Admin-Token` 헤더로 인증한다.
 * 토큰 값은 환경변수 `MOCKSERVER_ADMIN_TOKEN`으로 지정하며,
 * 미설정 시 개발용 기본값 `"dev-only-token"` 을 사용한다.
 * **운영 환경에서는 반드시 환경변수로 토큰을 변경해야 한다.**
 *
 * ```kotlin
 * // baseUrl == "http://localhost:9999"
 * val client = WebClient.create("http://localhost:9999")
 * // 모든 fixture 데이터를 초기 상태로 재적재
 * val result = client.post().uri("/admin/reset")
 *     .header("X-Admin-Token", "dev-only-token")
 *     .retrieve().awaitBody<Map<String, String>>()
 * // result["status"] == "ok"
 * ```
 *
 * @param jsonplaceholderService jsonplaceholder 데이터 서비스
 */
@RestController
@RequestMapping("/admin")
class AdminController(private val jsonplaceholderService: JsonplaceholderService) {
    companion object: KLogging() {
        private val adminToken = System.getenv("MOCKSERVER_ADMIN_TOKEN") ?: "dev-only-token"
    }

    /**
     * 모든 인메모리 데이터를 fixture 파일로부터 원자적으로 재적재한다.
     *
     * jsonplaceholder fixture(posts, comments, albums, photos, todos, users)를
     * 클래스패스 JSON 파일에서 다시 로드하여 인메모리 저장소를 초기 상태로 되돌린다.
     *
     * `X-Admin-Token` 헤더가 없거나 올바르지 않으면 401 응답을 반환한다.
     *
     * @param token `X-Admin-Token` 요청 헤더 (필수)
     * @return 재적재 완료 메시지 또는 401
     */
    @PostMapping("/reset")
    @CacheEvict(value = ["fixture-data"], allEntries = true)
    suspend fun reset(
        @RequestHeader("X-Admin-Token", required = false) token: String?,
    ): ResponseEntity<Map<String, String>> {
        if (token != adminToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        log.info { "Admin reset requested: reloading all fixtures..." }
        jsonplaceholderService.reloadFromFixtures()
        return ResponseEntity.ok(mapOf("status" to "ok", "message" to "All fixtures reloaded"))
    }
}
