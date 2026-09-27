package io.bluetape4k.retrofit2.clients.vertx

import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VertxCallFactoryLifecycleTest {

    companion object: KLogging()

    private val client = mockk<HttpClient>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(client)
    }

    @Test
    fun `factory close does not close the caller owned client`() {

        every { client.close() } returns Future.succeededFuture()

        vertxCallFactoryOf(client).close()

        verify(exactly = 0) { client.close() }
    }
}
