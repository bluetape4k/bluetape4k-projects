package io.bluetape4k.r2dbc.config

import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.support.uninitialized
import io.r2dbc.spi.ValidationDepth
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.toMono

@SpringBootTest
class R2dbcConfigurationTest {

    @Autowired
    private val client: R2dbcClient = uninitialized()

    @Test
    fun `context loading`() {
        client.shouldNotBeNull()

        runBlocking {
            client.databaseClient.inConnection { conn ->
                conn.validate(ValidationDepth.LOCAL).toMono()
            }.awaitSingle().shouldBeTrue()
        }
    }
}
