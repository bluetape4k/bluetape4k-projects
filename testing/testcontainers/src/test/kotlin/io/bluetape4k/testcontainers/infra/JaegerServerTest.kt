package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class JaegerServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `launch jaeger server`() {
        JaegerServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `launch jaeger server with default port`() {
        JaegerServer(useDefaultPort = true).use { server ->
            server.start()
            server.isRunning.shouldBeTrue()

            server.port shouldBeEqualTo JaegerServer.FRONTEND_PORT
        }
    }
}
