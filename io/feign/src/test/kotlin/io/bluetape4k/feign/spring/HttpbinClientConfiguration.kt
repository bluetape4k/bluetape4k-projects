package io.bluetape4k.feign.spring

import com.fasterxml.jackson.databind.json.JsonMapper
import feign.codec.Decoder
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import org.springframework.context.annotation.Bean

class HttpbinClientConfiguration {

    companion object: KLogging()

    @Bean
    fun jsonMapper(): JsonMapper = Jackson.defaultJsonMapper

    @Bean
    fun decoder(mapper: JsonMapper): Decoder = JacksonDecoder2(mapper)
}
