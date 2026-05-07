package io.bluetape4k.spring.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.spring.AbstractSpringTest
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.util.*

class Jackson2ObjectMapperBuilderCustomizerTest: AbstractSpringTest() {

    @Test
    fun `customizer 생성 성공`() {
        val customizer = jackson2ObjectMapperBuilderCustomizer {}
        customizer.shouldNotBeNull()
    }

    @Test
    fun `customizer 적용 후 ObjectMapper 빌드 성공`() {
        val customizer = jackson2ObjectMapperBuilderCustomizer {}
        val builder = Jackson2ObjectMapperBuilder()
        customizer.customize(builder)
        val mapper = builder.build<ObjectMapper>()
        mapper.shouldNotBeNull()
    }

    @Test
    fun `customizer에 추가 설정 적용`() {
        val customizer = jackson2ObjectMapperBuilderCustomizer {
            timeZone(TimeZone.getTimeZone("UTC"))
        }
        val builder = Jackson2ObjectMapperBuilder()
        customizer.customize(builder)
        val mapper = builder.build<ObjectMapper>()
        mapper.shouldNotBeNull()
    }

    @Test
    fun `기본 customizer로 직렬화 성공`() {
        data class Sample(val name: String, val value: Int)

        val customizer = jackson2ObjectMapperBuilderCustomizer {}
        val builder = Jackson2ObjectMapperBuilder()
        customizer.customize(builder)
        val mapper = builder.build<ObjectMapper>()

        val json = mapper.writeValueAsString(Sample("test", 42))
        json.shouldNotBeNull()
    }

    @Test
    fun `null 값은 직렬화에서 제외`() {
        data class Nullable(val name: String?, val value: Int)

        val customizer = jackson2ObjectMapperBuilderCustomizer {}
        val builder = Jackson2ObjectMapperBuilder()
        customizer.customize(builder)
        val mapper = builder.build<ObjectMapper>()

        val json = mapper.writeValueAsString(Nullable(null, 1))
        // NON_NULL 설정으로 name 필드 제외
        json.shouldNotBeNull()
    }
}
