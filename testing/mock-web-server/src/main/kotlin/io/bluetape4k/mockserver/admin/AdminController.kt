package io.bluetape4k.mockserver.admin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockserver.jsonplaceholder.JsonplaceholderService
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * mock-server 관리용 엔드포인트.
 *
 * fixture 재적재, 서버 상태 확인 등 운영 관리 기능을 제공한다.
 *
 * @param jsonplaceholderService jsonplaceholder 데이터 서비스
 */
@RestController
@RequestMapping("/admin")
class AdminController(private val jsonplaceholderService: JsonplaceholderService) {
    companion object : KLogging()

    /**
     * 모든 인메모리 데이터를 fixture 파일로부터 원자적으로 재적재한다.
     *
     * jsonplaceholder fixture(posts, comments, albums, photos, todos, users)를
     * 클래스패스 JSON 파일에서 다시 로드하여 인메모리 저장소를 초기 상태로 되돌린다.
     *
     * @return 재적재 완료 메시지
     */
    @PostMapping("/reset")
    @CacheEvict(value = ["fixture-data"], allEntries = true)
    fun reset(): ResponseEntity<Map<String, String>> {
        log.info { "Admin reset requested: reloading all fixtures..." }
        jsonplaceholderService.reloadFromFixtures()
        return ResponseEntity.ok(mapOf("status" to "ok", "message" to "All fixtures reloaded"))
    }
}
