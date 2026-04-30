package io.bluetape4k.testcontainers.aws.localstack

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class AbstractLocalStackServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    protected lateinit var localStack: LocalStackServer

    @BeforeAll
    fun beforeAll() {
        localStack = LocalStackServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if (this::localStack.isInitialized && localStack.isRunning) {
            localStack.close()
        }
    }
}
