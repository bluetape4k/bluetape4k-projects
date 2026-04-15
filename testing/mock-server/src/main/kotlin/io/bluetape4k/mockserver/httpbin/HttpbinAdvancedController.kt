package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.mockserver.httpbin.model.HttpbinResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * httpbin.org 고급 엔드포인트 시뮬레이터.
 *
 * 지연, 리다이렉트, 쿠키, 인증, 캐시, ETag 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinAdvancedController {
    companion object : KLogging()

    /**
     * 지정된 초만큼 응답을 지연한다. Virtual Threads 환경에서 Thread.sleep 사용 가능.
     *
     * @param seconds 지연 시간 (0..10)
     */
    @GetMapping("/delay/{seconds}")
    fun delay(@PathVariable seconds: Int, request: HttpServletRequest): HttpbinResponse {
        require(seconds in 0..10) { "delay must be 0..10, got: $seconds" }
        Thread.sleep(seconds.toLong() * 1000L)
        return request.toHttpbinResponse(method = "GET")
    }

    /**
     * n번 리다이렉트 후 /httpbin/get으로 이동한다.
     *
     * @param n 리다이렉트 횟수 (0 이상)
     */
    @GetMapping("/redirect/{n}")
    fun redirect(@PathVariable n: Int): ResponseEntity<Any> {
        require(n >= 0) { "n must be >= 0, got: $n" }
        val location = if (n > 0) URI("/httpbin/redirect/${n - 1}") else URI("/httpbin/get")
        return ResponseEntity.status(302)
            .location(location)
            .build()
    }

    /** 현재 요청의 쿠키를 에코한다. */
    @GetMapping("/cookies")
    fun cookies(request: HttpServletRequest): Map<String, Any> {
        val cookies = request.cookies?.associate { it.name to it.value } ?: emptyMap()
        return mapOf("cookies" to cookies)
    }

    /** 쿠키를 설정하고 /httpbin/cookies로 리다이렉트한다. */
    @GetMapping("/cookies/set")
    fun setCookies(
        @RequestParam params: Map<String, String>,
        response: HttpServletResponse,
    ): ResponseEntity<Any> {
        params.forEach { (name, value) ->
            response.addCookie(Cookie(name, value).apply { path = "/" })
        }
        return ResponseEntity.status(302).location(URI("/httpbin/cookies")).build()
    }

    /** 지정된 쿠키를 삭제하고 /httpbin/cookies로 리다이렉트한다. */
    @GetMapping("/cookies/delete")
    fun deleteCookies(
        @RequestParam params: Map<String, String>,
        response: HttpServletResponse,
    ): ResponseEntity<Any> {
        params.keys.forEach { name ->
            response.addCookie(
                Cookie(name, "").apply {
                    path = "/"
                    maxAge = 0
                }
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
    fun basicAuth(
        @PathVariable user: String,
        @PathVariable passwd: String,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val authHeader = request.getHeader("Authorization") ?: return ResponseEntity.status(401)
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
        } catch (e: Exception) {
            ResponseEntity.status(401).build()
        }
    }

    /** Bearer 토큰 인증을 검증한다. */
    @GetMapping("/bearer")
    fun bearer(request: HttpServletRequest): ResponseEntity<Any> {
        val authHeader = request.getHeader("Authorization") ?: return ResponseEntity.status(401).build()
        return if (authHeader.startsWith("Bearer ") && authHeader.length > 7) {
            val token = authHeader.removePrefix("Bearer ").trim()
            ResponseEntity.ok(mapOf("authenticated" to true, "token" to token))
        } else {
            ResponseEntity.status(401).build()
        }
    }

    /** If-Modified-Since 헤더 기반 캐시 응답 시뮬레이션. */
    @GetMapping("/cache")
    fun cache(request: HttpServletRequest): ResponseEntity<Any> {
        val ifModifiedSince = request.getHeader("If-Modified-Since")
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
    fun cacheControl(@PathVariable value: Int): ResponseEntity<Any> =
        ResponseEntity.ok()
            .header("Cache-Control", "public, max-age=$value")
            .body(mapOf("max-age" to value))

    /**
     * ETag 조건부 요청 시뮬레이션.
     *
     * @param etag ETag 값
     */
    @GetMapping("/etag/{etag}")
    fun etag(@PathVariable etag: String, request: HttpServletRequest): ResponseEntity<Any> {
        val ifNoneMatch = request.getHeader("If-None-Match")
        return if (ifNoneMatch != null && ifNoneMatch == "\"$etag\"") {
            ResponseEntity.status(304).build()
        } else {
            ResponseEntity.ok()
                .header(HttpHeaders.ETAG, "\"$etag\"")
                .body(mapOf("etag" to etag))
        }
    }
}
