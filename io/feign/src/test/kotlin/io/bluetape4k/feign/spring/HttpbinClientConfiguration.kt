package io.bluetape4k.feign.spring

import feign.codec.Decoder
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

class HttpbinClientConfiguration {

    companion object: KLogging()

    @Bean
    fun jsonMapper(): JsonMapper = Jackson.defaultJsonMapper

    @Bean
    fun decoder(mapper: JsonMapper): Decoder = JacksonDecoder2(mapper)
}
