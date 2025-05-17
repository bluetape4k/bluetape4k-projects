package io.bluetape4k.r2dbc.config

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.support.uninitialized
import io.r2dbc.spi.ValidationDepth
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.toMono

@SpringBootTest
class R2dbcConfigurationTest {

    companion object: KLoggingChannel()

    @Autowired
    private val client: R2dbcClient = uninitialized()

    @Test
    fun `context loading`() {
        client.shouldNotBeNull()

        runSuspendIO {
            client.databaseClient.inConnection { conn ->
                conn.validate(ValidationDepth.LOCAL).toMono()
            }.awaitSingle().shouldBeTrue()
        }
    }
}
