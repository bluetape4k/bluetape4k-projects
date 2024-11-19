package io.bluetape4k.spring.webflux.config

import io.bluetape4k.utils.Runtimex
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CustomWebClientConfig: AbstractWebClientConfig() {

    // @Value("\${spring.webflux.client.thread-count:4}")
    override var threadCount: Int = Runtimex.availableProcessors

}
