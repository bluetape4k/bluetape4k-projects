package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class Ignite2ServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `create ignite2 server`() {
        Ignite2Server().use {
            it.start()
            it.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `create ignite2 server with default port`() {
        Ignite2Server(useDefaultPort = true).use {
            it.start()
            it.isRunning.shouldBeTrue()
            it.port shouldBeEqualTo Ignite2Server.PORT
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Ignite2Server(image = " ") }
        assertFailsWith<IllegalArgumentException> { Ignite2Server(tag = " ") }
    }
}
