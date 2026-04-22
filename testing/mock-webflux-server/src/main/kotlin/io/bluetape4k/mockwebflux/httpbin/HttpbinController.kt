package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockwebflux.httpbin.model.HttpbinResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * httpbin.org 기본 엔드포인트 시뮬레이터 (WebFlux + Coroutines 버전).
 *
 * GET/POST/PUT/PATCH/DELETE, headers, ip, user-agent, uuid, anything, status 엔드포인트를 제공한다.
 * 모든 엔드포인트는 `/httpbin` prefix 아래에 위치하며, suspend 함수로 구현된다.
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinController {
    companion object: KLogging()

    /**
     * GET 요청 정보를 그대로 반환한다.
     */
    @GetMapping("/get")
    suspend fun get(request: ServerHttpRequest): HttpbinResponse =
        request.toHttpbinResponse(method = "GET")

    /**
     * POST 요청 정보(body 포함)를 반환한다.
     */
    @PostMapping("/post")
    suspend fun post(
        request: ServerHttpRequest,
        @RequestBody(required = false) body: String?,
    ): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "POST")

    /**
     * PUT 요청 정보(body 포함)를 반환한다.
     */
    @PutMapping("/put")
    suspend fun put(
        request: ServerHttpRequest,
        @RequestBody(required = false) body: String?,
    ): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "PUT")

    /**
     * PATCH 요청 정보(body 포함)를 반환한다.
     */
    @PatchMapping("/patch")
    suspend fun patch(
        request: ServerHttpRequest,
        @RequestBody(required = false) body: String?,
    ): HttpbinResponse =
        request.toHttpbinResponse(body = body, method = "PATCH")

    /**
     * DELETE 요청 정보를 반환한다.
     */
    @DeleteMapping("/delete")
    suspend fun delete(request: ServerHttpRequest): HttpbinResponse =
        request.toHttpbinResponse(method = "DELETE")

    /**
     * 요청 헤더 전체를 반환한다.
     */
    @GetMapping("/headers")
    suspend fun headers(request: ServerHttpRequest): Map<String, Any> =
        mapOf("headers" to request.toHeaderMap())

    /**
     * 클라이언트 IP 주소를 반환한다.
     */
    @GetMapping("/ip")
    suspend fun ip(request: ServerHttpRequest): Map<String, String> {
        val origin = request.remoteAddress?.let {
            it.address?.hostAddress ?: it.hostString
        } ?: ""
        return mapOf("origin" to origin)
    }

    /**
     * User-Agent 헤더 값을 반환한다.
     */
    @GetMapping("/user-agent")
    suspend fun userAgent(request: ServerHttpRequest): Map<String, String> =
        mapOf("user-agent" to (request.headers.getFirst("User-Agent") ?: ""))

    /**
     * 랜덤 UUID를 생성하여 반환한다.
     */
    @GetMapping("/uuid")
    suspend fun uuid(): Map<String, String> =
        mapOf("uuid" to UUID.randomUUID().toString())

    /**
     * 임의의 경로/메서드에 대해 요청 정보를 그대로 반환한다.
     */
    @RequestMapping("/anything/**")
    suspend fun anything(
        request: ServerHttpRequest,
        @RequestBody(required = false) body: String?,
    ): HttpbinResponse =
        request.toHttpbinResponse(body = body)

    /**
     * 지정된 HTTP 상태 코드를 반환한다.
     *
     * @param code HTTP 상태 코드 (100..599)
     */
    @RequestMapping("/status/{code}")
    suspend fun status(@PathVariable code: Int): ResponseEntity<Any> {
        require(code in 100..599) { "Invalid status code: $code. Must be 100..599" }
        return ResponseEntity.status(code).build()
    }

    /**
     * n 바이트의 랜덤 바이트를 반환한다. 캐시 미적용 (매 요청마다 랜덤).
     *
     * @param n 바이트 수 (1..102400)
     */
    @GetMapping("/bytes/{n}")
    suspend fun bytes(@PathVariable n: Int): ResponseEntity<ByteArray> {
        require(n in 1..102400) { "n must be between 1 and 102400, got: $n" }
        val bytes = ByteArray(n).also { ThreadLocalRandom.current().nextBytes(it) }
        return ResponseEntity.ok()
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
    }
}
