package io.bluetape4k.testcontainers.aws.ministack

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

abstract class AbstractMiniStackServiceTest : AbstractContainerTest() {

    companion object: KLogging()

    protected lateinit var miniStack: MiniStackServer

    @BeforeAll
    fun beforeAll() {
        miniStack = MiniStackServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if(this::miniStack.isInitialized && miniStack.isRunning) {
            miniStack.close()
        }
    }
}
