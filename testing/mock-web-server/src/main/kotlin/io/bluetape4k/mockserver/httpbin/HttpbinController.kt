package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.httpbin.model.HttpbinResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * httpbin.org 기본 엔드포인트 시뮬레이터.
 *
 * GET/POST/PUT/PATCH/DELETE, headers, ip, user-agent, uuid, anything, status 엔드포인트를 제공한다.
 * 모든 엔드포인트는 `/httpbin` prefix 아래에 위치한다.
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinController {
    companion object: KLogging()

    /**
     * GET 요청 정보를 그대로 반환한다.
     */
    @GetMapping("/get")
    fun get(request: HttpServletRequest): HttpbinResponse =
        request.toHttpbinResponse(method = "GET")

    /**
     * POST 요청 정보(body 포함)를 반환한다.
     */
    @PostMapping("/post")
    fun post(request: HttpServletRequest, @RequestBody(required = false) body: String?): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "POST")

    /**
     * PUT 요청 정보(body 포함)를 반환한다.
     */
    @PutMapping("/put")
    fun put(request: HttpServletRequest, @RequestBody(required = false) body: String?): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "PUT")

    /**
     * PATCH 요청 정보(body 포함)를 반환한다.
     */
    @PatchMapping("/patch")
    fun patch(request: HttpServletRequest, @RequestBody(required = false) body: String?): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "PATCH")

    /**
     * DELETE 요청 정보를 반환한다.
     */
    @DeleteMapping("/delete")
    fun delete(request: HttpServletRequest): HttpbinResponse =
        request.toHttpbinResponse(method = "DELETE")

    /**
     * 요청 헤더 전체를 반환한다.
     */
    @GetMapping("/headers")
    fun headers(request: HttpServletRequest): Map<String, Any> =
        mapOf("headers" to request.toHeaderMap())

    /**
     * 클라이언트 IP 주소를 반환한다.
     */
    @GetMapping("/ip")
    fun ip(request: HttpServletRequest): Map<String, String> =
        mapOf("origin" to (request.remoteAddr ?: ""))

    /**
     * User-Agent 헤더 값을 반환한다.
     */
    @GetMapping("/user-agent")
    fun userAgent(request: HttpServletRequest): Map<String, String> =
        mapOf("user-agent" to (request.getHeader("User-Agent") ?: ""))

    /**
     * 랜덤 UUID를 생성하여 반환한다.
     */
    @GetMapping("/uuid")
    fun uuid(): Map<String, String> =
        mapOf("uuid" to UUID.randomUUID().toString())

    /**
     * 임의의 경로/메서드에 대해 요청 정보를 그대로 반환한다.
     */
    @RequestMapping("/anything/**")
    fun anything(request: HttpServletRequest, @RequestBody(required = false) body: String?): HttpbinResponse =
        request.toHttpbinResponse(body = body)

    /**
     * 지정된 HTTP 상태 코드를 반환한다.
     *
     * @param code HTTP 상태 코드 (100..599)
     */
    @RequestMapping("/status/{code}")
    fun status(@PathVariable code: Int): ResponseEntity<Any> {
        require(code in 100..599) { "Invalid status code: $code. Must be 100..599" }
        return ResponseEntity.status(code).build()
    }

    /**
     * n 바이트의 랜덤 바이트를 반환한다. 캐시 미적용 (매 요청마다 랜덤).
     *
     * @param n 바이트 수 (1..102400)
     */
    @GetMapping("/bytes/{n}")
    fun bytes(@PathVariable n: Int): ResponseEntity<ByteArray> {
        require(n in 1..102400) { "n must be between 1 and 102400, got: $n" }
        val bytes = ByteArray(n).also { java.util.concurrent.ThreadLocalRandom.current().nextBytes(it) }
        return ResponseEntity.ok()
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
    }
}
