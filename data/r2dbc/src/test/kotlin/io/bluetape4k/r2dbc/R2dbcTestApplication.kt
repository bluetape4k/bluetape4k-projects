package io.bluetape4k.r2dbc

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.config.R2dbcClientAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
@ImportAutoConfiguration(classes = [R2dbcClientAutoConfiguration::class])
class R2dbcTestApplication {

    companion object: KLoggingChannel()

}
