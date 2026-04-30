package io.bluetape4k.testcontainers.aws.floci

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.FlociServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

@Suppress("DEPRECATION")
abstract class AbstractFlociServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    protected lateinit var floci: FlociServer

    @BeforeAll
    fun beforeAll() {
        floci = FlociServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if (this::floci.isInitialized && floci.isRunning) {
            floci.close()
        }
    }
}
