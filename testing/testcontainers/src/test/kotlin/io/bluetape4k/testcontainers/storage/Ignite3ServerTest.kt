package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

@Disabled("사용성이 낮아서 테스트에서 제외합니다")
class Ignite3ServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `create ignite3 server`() {
        Ignite3Server().use {
            it.start()
            it.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `create ignite3 server with default port`() {
        Ignite3Server(useDefaultPort = true).use {
            it.start()
            it.isRunning.shouldBeTrue()
            it.port shouldBeEqualTo Ignite3Server.CLIENT_PORT
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Ignite3Server(image = " ") }
        assertFailsWith<IllegalArgumentException> { Ignite3Server(tag = " ") }
    }
}
