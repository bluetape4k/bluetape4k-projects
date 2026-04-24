package io.bluetape4k.mockserver.admin

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 서버 상태 확인용 ping 엔드포인트.
 *
 * Testcontainers의 [org.testcontainers.containers.wait.strategy.HttpWaitStrategy]가
 * `/ping` 경로에 HTTP 200 응답을 확인할 때 사용된다.
 */
@RestController
class PingController {

    /**
     * 서버 생존 여부를 확인하는 ping 엔드포인트.
     *
     * @return `"pong"` 문자열 응답
     */
    @GetMapping("/ping", produces = ["text/plain"])
    fun ping(): ResponseEntity<String> =
        ResponseEntity.ok("pong")
}
