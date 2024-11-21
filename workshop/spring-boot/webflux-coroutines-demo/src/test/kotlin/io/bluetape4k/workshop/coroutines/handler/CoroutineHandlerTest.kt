package io.bluetape4k.workshop.coroutines.handler

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.coroutines.model.Banner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CoroutineHandlerTest(
    @Autowired private val client: WebTestClient,
): CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("handler-test")) {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    private val banner = Banner("제목", "동해물과 백두산이 마르고 닳도록")

    @Test
    fun index() = runSuspendTest {
        client.get().uri("/")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun suspending() = runSuspendTest {
        client.get().uri("/suspend")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Banner>().isEqualTo(banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun deferred() = runSuspendTest {
        client.get().uri("/deferred")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Banner>().isEqualTo(banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `sequential flow`() = runSuspendTest {
        client.get().uri("/sequential-flow")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBodyList<Banner>().contains(banner, banner, banner, banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `concurrent flow`() = runSuspendTest {
        client.get().uri("/concurrent-flow")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBodyList<Banner>().contains(banner, banner, banner, banner)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun error() = runSuspendTest {
        client.get().uri("/error")
            .exchange()
            .expectStatus().is5xxServerError
    }
}
