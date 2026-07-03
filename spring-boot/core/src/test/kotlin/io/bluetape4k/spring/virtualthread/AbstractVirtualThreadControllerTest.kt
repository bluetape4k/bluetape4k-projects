package io.bluetape4k.spring.virtualthread

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class AbstractVirtualThreadControllerTest {

    @AfterEach
    fun tearDown() {
        AbstractVirtualThreadController.shutdownVirtualThreadExecutor()
    }

    @Test
    fun `controller destroy shuts down current virtual thread executor`() {
        val executor = AbstractVirtualThreadController.virtualThreadExecutor
        executor.isShutdown.shouldBeFalse()

        TestVirtualThreadController().closeVirtualThreadExecutor()

        executor.isShutdown.shouldBeTrue()
    }

    @Test
    fun `executor accessor recreates executor after shutdown`() {
        val closedExecutor = AbstractVirtualThreadController.virtualThreadExecutor
        TestVirtualThreadController().closeVirtualThreadExecutor()
        closedExecutor.isShutdown.shouldBeTrue()

        val replacementExecutor = AbstractVirtualThreadController.virtualThreadExecutor

        replacementExecutor.isShutdown.shouldBeFalse()
        (replacementExecutor === closedExecutor).shouldBeFalse()
    }

    @Test
    fun `spring context shutdown closes controller executor`() {
        val executor = AbstractVirtualThreadController.virtualThreadExecutor

        contextRunner.run { context ->
            context.getBean(TestVirtualThreadController::class.java)
            executor.isShutdown.shouldBeFalse()
        }

        executor.isShutdown.shouldBeTrue()
    }

    private class TestVirtualThreadController: AbstractVirtualThreadController()

    @Configuration(proxyBeanMethods = false)
    private class TestConfiguration {

        @Bean
        fun testVirtualThreadController(): TestVirtualThreadController =
            TestVirtualThreadController()
    }

    private companion object {
        val contextRunner = ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration::class.java)
    }
}
