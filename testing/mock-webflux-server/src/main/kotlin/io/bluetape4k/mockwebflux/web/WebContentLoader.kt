package io.bluetape4k.mockwebflux.web

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

/**
 * 웹 HTML 컨텐츠 로더 (WebFlux).
 *
 * Spring `@Cacheable` 는 Kotlin `suspend` 함수와 호환되지 않으므로,
 * 로더는 non-suspend 함수로 유지하고 호출 측에서 `withContext(Dispatchers.IO)` 로 래핑한다 (Method A).
 *
 * 캐시 이름은 `"web-content"` (MVC 버전의 `"html-content"` 와는 의도적으로 분리).
 */
@Service
class WebContentLoader {
    companion object: KLogging()

    /**
     * 지정된 이름의 HTML 파일을 로드한다.
     *
     * Spring `@Cacheable` 로 결과를 메모리 캐시에 저장한다.
     *
     * @param name HTML 파일명 (확장자 제외)
     * @return HTML 문자열
     * @throws IllegalArgumentException 이름이 공백이거나 파일이 존재하지 않는 경우
     */
    @Cacheable("web-content", key = "#name")
    fun load(name: String): String {
        name.requireNotBlank("name")
        val resource = ClassPathResource("web/html/$name.html")
        require(resource.exists()) { "HTML page not found: $name" }
        return resource.inputStream.reader(Charsets.UTF_8).readText()
    }
}
