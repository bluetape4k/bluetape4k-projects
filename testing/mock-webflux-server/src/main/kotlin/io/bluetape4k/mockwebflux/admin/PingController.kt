package io.bluetape4k.mockwebflux.admin

import io.bluetape4k.logging.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 서버 상태 확인용 ping 엔드포인트 (WebFlux).
 *
 * Testcontainers 의 [org.testcontainers.containers.wait.strategy.HttpWaitStrategy] 가
 * `/ping` 경로에 HTTP 200 응답을 확인할 때 사용된다.
 *
 * 단순 상수 반환이므로 suspend 가 필요하지 않다.
 */
@RestController
class PingController {
    companion object: KLogging()

    /**
     * 서버 생존 여부를 확인하는 ping 엔드포인트.
     *
     * @return `"pong"` 문자열 응답
     */
    @GetMapping("/ping", produces = ["text/plain"])
    fun ping(): ResponseEntity<String> = ResponseEntity.ok("pong")
}
