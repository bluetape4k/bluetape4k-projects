package io.bluetape4k.testcontainers.infra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class ZipkinServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `launch zipkin server`() {
        ZipkinServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `launch zipkin server with default port`() {
        ZipkinServer(useDefaultPort = true).use { server ->
            server.start()
            server.isRunning.shouldBeTrue()

            server.port shouldBeEqualTo ZipkinServer.PORT
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ZipkinServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ZipkinServer(tag = " ") }
    }
}
