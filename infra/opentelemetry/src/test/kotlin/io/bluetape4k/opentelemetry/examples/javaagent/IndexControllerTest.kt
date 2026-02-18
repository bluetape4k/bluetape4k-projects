package io.bluetape4k.opentelemetry.examples.javaagent

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.opentelemetry.AbstractOtelTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(
    classes = [OtelSpringBootApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractOtelTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `context loading`() {
        // Nothing to do
    }

    @Test
    fun `ping rest api`() = runSuspendIO {
        repeat(REPEAT_SIZE) {
            remotePing()
            delay(1000L)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `ping async rest api`() = runSuspendIO {
        val jobs = List(5) {
            launch {
                remotePing()
                delay(1000L)
            }
        }
        jobs.joinAll()
    }

    private suspend fun remotePing() {
        log.debug { "call remote api. path=/ping" }

        client.get()
            .uri("/ping")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<String>()
            .returnResult()
            .responseBody.shouldNotBeNull() shouldBeEqualTo "pong"
    }
}
