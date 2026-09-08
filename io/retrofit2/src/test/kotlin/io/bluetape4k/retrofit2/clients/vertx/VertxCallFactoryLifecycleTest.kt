package io.bluetape4k.retrofit2.clients.vertx

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.Future
import io.vertx.core.http.HttpClient
import org.junit.jupiter.api.Test

class VertxCallFactoryLifecycleTest {

    @Test
    fun `factory close does not close the caller owned client`() {
        val client = mockk<HttpClient>()
        every { client.close() } returns Future.succeededFuture()

        vertxCallFactoryOf(client).close()

        verify(exactly = 0) { client.close() }
    }
}
