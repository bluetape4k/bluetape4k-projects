package io.bluetape4k.mockserver.web

import io.bluetape4k.logging.KLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ThreadLocalRandom

/**
 * 목업 웹 컨텐츠 컨트롤러.
 *
 * 정적 HTML 목업 페이지를 제공한다. 외부 리소스 의존성 없음.
 */
@RestController
@RequestMapping("/web")
class WebContentController(private val loader: WebContentLoader) {
    companion object : KLogging() {
        private val HTML_NAMES = listOf("home", "naver", "google", "login", "article")
    }

    /**
     * 무작위 HTML 페이지를 반환한다.
     */
    @GetMapping("/random", produces = [MediaType.TEXT_HTML_VALUE])
    fun random(): String =
        loader.load(HTML_NAMES[ThreadLocalRandom.current().nextInt(HTML_NAMES.size)])

    /**
     * 지정된 이름의 HTML 페이지를 반환한다.
     *
     * @param name 페이지 이름 (home/naver/google/login/article)
     * @return HTML 페이지 또는 404
     */
    @GetMapping("/{name}", produces = [MediaType.TEXT_HTML_VALUE])
    fun byName(@PathVariable name: String): ResponseEntity<String> =
        runCatching { ResponseEntity.ok(loader.load(name)) }
            .getOrElse { ResponseEntity.notFound().build() }
}
