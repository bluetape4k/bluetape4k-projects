package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * 클래스패스에서 jsonplaceholder fixture 데이터를 로드하는 컴포넌트.
 *
 * Jackson 3 [JsonMapper]를 사용하여 JSON 파일을 타입 안전하게 역직렬화한다.
 *
 * @param jsonMapper Spring Boot 4 자동 구성된 Jackson 3 JsonMapper 빈
 */
@Component
class FixtureLoader(private val jsonMapper: JsonMapper) {
    companion object : KLogging()

    /**
     * 지정된 경로의 JSON 파일을 리스트로 역직렬화한다.
     *
     * CGLib 프록시 호환을 위해 [inline] 대신 [Class] 파라미터를 사용한다.
     *
     * @param T 역직렬화할 대상 타입
     * @param resourcePath 클래스패스 리소스 경로 (예: `jsonplaceholder/posts.json`)
     * @param elementType 역직렬화할 요소의 클래스 타입
     * @return 역직렬화된 엔티티 목록
     */
    fun <T> load(resourcePath: String, elementType: Class<T>): List<T> {
        val resource = ClassPathResource(resourcePath)
        val listType = jsonMapper.typeFactory.constructCollectionType(List::class.java, elementType)
        return jsonMapper.readValue(resource.inputStream, listType)
    }
}
