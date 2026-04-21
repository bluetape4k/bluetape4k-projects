package io.bluetape4k.mockwebflux.config

import io.bluetape4k.logging.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer
import tools.jackson.databind.json.JsonMapper

/**
 * WebFlux Jackson 3 코덱 설정.
 *
 * Spring Boot 4 / Spring Framework 7 WebFlux 에서 Jackson 3 [JsonMapper] 를
 * 명시적으로 등록한다.
 *
 * Spring 7 은 Jackson 3 전용 [JacksonJsonEncoder] / [JacksonJsonDecoder] 를 제공한다.
 * (기존 Jackson 2 용 `Jackson2JsonEncoder` / `Jackson2JsonDecoder` 는 deprecated.)
 */
@Configuration
class WebFluxJacksonConfig: WebFluxConfigurer {
    companion object: KLogging()

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(jsonMapper))
        configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(jsonMapper))
    }
}
