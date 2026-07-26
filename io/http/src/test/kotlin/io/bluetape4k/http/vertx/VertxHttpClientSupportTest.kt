package io.bluetape4k.http.vertx

import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.closeDefaultVertx
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientAgent
import io.vertx.core.http.HttpClientOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class VertxHttpClientSupportTest: AbstractHttpTest() {

    private val vertx = mockk<Vertx>()
    private val client = mockk<HttpClientAgent>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(vertx, client)
    }

    @AfterEach
    fun closeManagedResources() {
        closeDefaultVertxHttpClient().asCompletableFuture().get(5, TimeUnit.SECONDS)
        closeDefaultVertx().asCompletableFuture().get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `vertxHttpClientOf uses explicit Vertx owner`() {
        val options = HttpClientOptions()
        every { vertx.createHttpClient(options) } returns client

        val actual = vertxHttpClientOf(vertx, options)

        actual shouldBeSameInstanceAs client
        verify(exactly = 1) { vertx.createHttpClient(options) }
    }

    @Test
    fun `defaultVertxHttpClient is managed until explicitly closed`() {
        val first = defaultVertxHttpClient
        val second = defaultVertxHttpClient

        second shouldBeSameInstanceAs first

        closeDefaultVertxHttpClient().asCompletableFuture().get(5, TimeUnit.SECONDS)

        val recreated = defaultVertxHttpClient
        recreated shouldNotBeSameInstanceAs first
        recreated shouldBeSameInstanceAs defaultVertxHttpClient
    }
}
