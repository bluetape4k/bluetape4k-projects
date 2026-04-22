package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockwebflux.httpbin.model.HttpbinResponse
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.delay
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * httpbin.org 고급 엔드포인트 시뮬레이터 (WebFlux + Coroutines 버전).
 *
 * 지연, 리다이렉트, 쿠키, 인증, 캐시, ETag 엔드포인트를 제공한다.
 * servlet 버전의 `Thread.sleep()` 은 `kotlinx.coroutines.delay()` 로 대체되어
 * Reactor Netty 이벤트 루프를 블로킹하지 않는다.
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinAdvancedController {
    companion object: KLogging()

    /**
     * 지정된 시간만큼 응답을 지연한다. 소수점을 사용하면 밀리초 단위 지연이 가능하다.
     *
     * Netty 이벤트 루프 블로킹을 방지하기 위해 `kotlinx.coroutines.delay` 를 사용한다.
     *
     * 예: `1` → 1초, `0.5` → 500ms, `1.5` → 1500ms
     *
     * @param seconds 지연 시간 (0.0..10.0, 소수점 허용)
     */
    @GetMapping("/delay/{seconds}")
    suspend fun delay(
        @PathVariable seconds: Double,
        request: ServerHttpRequest,
    ): HttpbinResponse {
        require(seconds in 0.0..10.0) { "delay must be 0.0..10.0, got: $seconds" }
        delay((seconds * 1000.0).toLong())
        return request.toHttpbinResponse(method = "GET")
    }

    /**
     * n번 리다이렉트 후 /httpbin/get으로 이동한다.
     *
     * @param n 리다이렉트 횟수 (0 이상)
     */
    @GetMapping("/redirect/{n}")
    suspend fun redirect(@PathVariable n: Int): ResponseEntity<Any> {
        require(n >= 0) { "n must be >= 0, got: $n" }
        val location = if (n > 0) URI("/httpbin/redirect/${n - 1}") else URI("/httpbin/get")
        return ResponseEntity.status(302)
            .location(location)
            .build()
    }

    /** 현재 요청의 쿠키를 에코한다. */
    @GetMapping("/cookies")
    suspend fun cookies(request: ServerHttpRequest): Map<String, Any> {
        val cookies = request.cookies.entries
            .associate { (name, values) -> name to (values.firstOrNull()?.value ?: "") }
        return mapOf("cookies" to cookies)
    }

    /** 쿠키를 설정하고 /httpbin/cookies로 리다이렉트한다. */
    @GetMapping("/cookies/set")
    suspend fun setCookies(
        @RequestParam params: Map<String, String>,
        response: ServerHttpResponse,
    ): ResponseEntity<Any> {
        params.forEach { (name, value) ->
            response.addCookie(
                ResponseCookie.from(name, value)
                    .path("/")
                    .build()
            )
        }
        return ResponseEntity.status(302).location(URI("/httpbin/cookies")).build()
    }

    /** 지정된 쿠키를 삭제하고 /httpbin/cookies로 리다이렉트한다. */
    @GetMapping("/cookies/delete")
    suspend fun deleteCookies(
        @RequestParam params: Map<String, String>,
        response: ServerHttpResponse,
    ): ResponseEntity<Any> {
        params.keys.forEach { name ->
            response.addCookie(
                ResponseCookie.from(name, "")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build()
            )
        }
        return ResponseEntity.status(302).location(URI("/httpbin/cookies")).build()
    }

    /**
     * Basic 인증을 검증한다.
     *
     * @param user 기대 사용자명
     * @param passwd 기대 패스워드
     */
    @GetMapping("/basic-auth/{user}/{passwd}")
    suspend fun basicAuth(
        @PathVariable user: String,
        @PathVariable passwd: String,
        request: ServerHttpRequest,
    ): ResponseEntity<Any> {
        user.requireNotBlank("user")
        passwd.requireNotBlank("passwd")

        val authHeader = request.headers.getFirst("Authorization")
            ?: return ResponseEntity.status(401)
                .header("WWW-Authenticate", "Basic realm=\"Fake Realm\"")
                .build()

        return try {
            val encoded = authHeader.removePrefix("Basic ").trim()
            val decoded = String(Base64.getDecoder().decode(encoded))
            val (reqUser, reqPass) = decoded.split(":", limit = 2)
            if (reqUser == user && reqPass == passwd) {
                ResponseEntity.ok(mapOf("authenticated" to true, "user" to user))
            } else {
                ResponseEntity.status(401).build()
            }
        } catch (e: IllegalArgumentException) {
            // Base64 디코딩 실패 또는 ":" 구분자 없음
            ResponseEntity.status(401).build()
        }
    }

    /** Bearer 토큰 인증을 검증한다. */
    @GetMapping("/bearer")
    suspend fun bearer(request: ServerHttpRequest): ResponseEntity<Any> {
        val authHeader = request.headers.getFirst("Authorization")
            ?: return ResponseEntity.status(401).build()
        return if (authHeader.startsWith("Bearer ") && authHeader.length > 7) {
            val token = authHeader.removePrefix("Bearer ").trim()
            ResponseEntity.ok(mapOf("authenticated" to true, "token" to token))
        } else {
            ResponseEntity.status(401).build()
        }
    }

    /** If-Modified-Since 헤더 기반 캐시 응답 시뮬레이션. */
    @GetMapping("/cache")
    suspend fun cache(request: ServerHttpRequest): ResponseEntity<Any> {
        val ifModifiedSince = request.headers.getFirst("If-Modified-Since")
        return if (ifModifiedSince != null) {
            ResponseEntity.status(304).build()
        } else {
            val lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())
            ResponseEntity.ok()
                .header("Last-Modified", lastModified)
                .header("Cache-Control", "public")
                .body(mapOf("cached" to false))
        }
    }

    /** Cache-Control max-age 헤더를 설정한다. */
    @GetMapping("/cache/{value}")
    suspend fun cacheControl(@PathVariable value: Int): ResponseEntity<Any> =
        ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=$value")
            .body(mapOf("max-age" to value))

    /**
     * ETag 조건부 요청 시뮬레이션.
     *
     * @param etag ETag 값
     */
    @GetMapping("/etag/{etag}")
    suspend fun etag(
        @PathVariable etag: String,
        request: ServerHttpRequest,
    ): ResponseEntity<Any> {
        etag.requireNotBlank("etag")
        val ifNoneMatch = request.headers.getFirst("If-None-Match")
        return if (ifNoneMatch != null && ifNoneMatch == "\"$etag\"") {
            ResponseEntity.status(304).build()
        } else {
            ResponseEntity.ok()
                .header(HttpHeaders.ETAG, "\"$etag\"")
                .body(mapOf("etag" to etag))
        }
    }
}
