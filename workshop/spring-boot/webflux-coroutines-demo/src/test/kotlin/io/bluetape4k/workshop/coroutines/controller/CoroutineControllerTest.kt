package io.bluetape4k.workshop.coroutines.controller

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.coroutines.model.Banner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CoroutineControllerTest(
    @Autowired private val client: WebTestClient,
): CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("controller-test")) {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private fun currentCoroutineName(): String? = coroutineContext[CoroutineName]?.name

    private val banner = Banner("제목", "동해물과 백두산이 마르고 닳도록")

    private fun clientGet(uri: String) =
        client.get().uri(uri).accept(MediaType.APPLICATION_JSON)

    private fun clientPost(uri: String) =
        client.post().uri(uri).accept(MediaType.APPLICATION_JSON)

    @Test
    fun index() = runSuspendIO {
        clientGet("/controller/")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Banner>()
    }

    @Test
    fun suspending() = runSuspendIO {
        log.debug { "Call suspending ... coroutineName=[${currentCoroutineName()}]" }
        clientGet("/controller/suspend")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Banner>().isEqualTo(banner)
    }

    @Test
    fun defererred() = runSuspendIO {
        log.debug { "Call defererred ... coroutineName=[${currentCoroutineName()}]" }
        clientGet("/controller/deferred")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Banner>().isEqualTo(banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `sequential flow`() = runSuspendIO {
        log.debug { "Call sequential flow ... coroutineName=[${currentCoroutineName()}]" }
        clientGet("/controller/sequential-flow")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBodyList<Banner>().contains(banner, banner, banner, banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `concurrent flow`() = runSuspendIO {
        log.debug { "Call concurrent flow ... coroutineName=[${currentCoroutineName()}]" }
        clientGet("/controller/concurrent-flow")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBodyList<Banner>().contains(banner, banner, banner, banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun error() = runSuspendIO {
        log.debug { "Call error ... coroutineName=[${currentCoroutineName()}]" }
        clientGet("/controller/error")
            .exchange()
            .expectStatus().is5xxServerError
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `request as flow`() = runSuspendIO {
        log.debug { "Call request as flow ... coroutineName=[${currentCoroutineName()}]" }
        val request = (1..5).asFlow()
            .onEach {
                delay(10)
                log.debug { "request node: $it. coroutineName=[${currentCoroutineName()}]" }
            }
            .map {
                JsonNodeFactory.instance.numberNode(it)
            }

        clientPost("/controller/request-as-flow")
            .body(request)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<String>().isEqualTo("12345")
    }
}         
