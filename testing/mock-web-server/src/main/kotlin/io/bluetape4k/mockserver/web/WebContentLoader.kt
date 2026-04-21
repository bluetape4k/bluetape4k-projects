package io.bluetape4k.mockserver.web

import io.bluetape4k.logging.KLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

/**
 * 웹 HTML 컨텐츠 로더.
 *
 * self-injection 없이 @Cacheable을 적용하기 위해 별도 Bean으로 분리.
 * html-content 캐시에 최대 50개 항목을 저장한다.
 */
@Service
class WebContentLoader {
    companion object : KLogging()

    /**
     * 지정된 이름의 HTML 파일을 로드한다.
     *
     * @param name HTML 파일명 (확장자 제외)
     * @return HTML 문자열
     * @throws IllegalArgumentException 파일이 존재하지 않는 경우
     */
    @Cacheable("html-content", key = "#name")
    fun load(name: String): String {
        val resource = ClassPathResource("web/html/$name.html")
        require(resource.exists()) { "HTML page not found: $name" }
        return resource.inputStream.reader(Charsets.UTF_8).readText()
    }
}
