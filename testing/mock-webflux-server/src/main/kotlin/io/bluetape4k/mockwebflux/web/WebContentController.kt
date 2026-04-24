package io.bluetape4k.mockwebflux.web

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ThreadLocalRandom

/**
 * 목업 웹 컨텐츠 컨트롤러 (WebFlux suspend).
 *
 * 정적 HTML 목업 페이지를 제공한다. 외부 리소스 의존성 없음.
 * 파일 I/O 는 [Dispatchers.IO] 로 오프로드하여 이벤트 루프를 블로킹하지 않는다.
 *
 * ```kotlin
 * // baseUrl == "http://localhost:9999"
 * val client = WebClient.create("http://localhost:9999")
 * // 무작위 HTML 페이지 수신
 * val randomHtml = client.get().uri("/web/random")
 *     .accept(MediaType.TEXT_HTML)
 *     .retrieve().awaitBody<String>()
 * // 특정 페이지 수신 (home/naver/google/login/article)
 * val homeHtml = client.get().uri("/web/home")
 *     .accept(MediaType.TEXT_HTML)
 *     .retrieve().awaitBody<String>()
 * ```
 */
@RestController
@RequestMapping("/web")
class WebContentController(private val loader: WebContentLoader) {
    companion object: KLogging() {
        private val HTML_NAMES = listOf("home", "naver", "google", "login", "article")
    }

    /**
     * 무작위 HTML 페이지를 반환한다.
     *
     * @return 임의의 HTML 페이지 문자열
     */
    @GetMapping("/random", produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun random(): String = withContext(Dispatchers.IO) {
        loader.load(HTML_NAMES[ThreadLocalRandom.current().nextInt(HTML_NAMES.size)])
    }

    /**
     * 지정된 이름의 HTML 페이지를 반환한다.
     *
     * @param name 페이지 이름 (home/naver/google/login/article)
     * @return HTML 페이지 또는 404
     */
    @GetMapping("/{name}", produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun byName(@PathVariable name: String): ResponseEntity<String> {
        name.requireNotBlank("name")
        return runCatching { ResponseEntity.ok(withContext(Dispatchers.IO) { loader.load(name) }) }
            .getOrElse { ResponseEntity.notFound().build() }
    }
}
