package io.bluetape4k.grpc.examples.helloworld

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GreeterServiceTest {

    companion object: KLogging() {
        private const val PROCESS_NAME = "greeter.service"
    }

    private val server = GreeterServer(PROCESS_NAME)
    private val client = GreeterClient(PROCESS_NAME)

    @BeforeAll
    fun setup() {
        server.start()
    }

    @AfterAll
    fun cleanup() {
        client.close()
        server.close()
    }

    @Test
    fun `initialize server and client`() {
        server.isRunning.shouldBeTrue()
    }

    @Test
    fun `say hello with grpc`() = runSuspendTest {
        val message = client.sayHello("Debop")
        message shouldBeEqualTo "Hello Debop"
    }
}
