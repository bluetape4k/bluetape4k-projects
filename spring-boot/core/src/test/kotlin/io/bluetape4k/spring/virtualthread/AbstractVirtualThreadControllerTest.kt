package io.bluetape4k.spring.virtualthread

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBe
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class AbstractVirtualThreadControllerTest {

    private companion object: KLogging() {
        val contextRunner = ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration::class.java)
    }

    private class TestVirtualThreadController: AbstractVirtualThreadController()

    @Configuration(proxyBeanMethods = false)
    private class TestConfiguration {

        @Bean
        fun testVirtualThreadController(): TestVirtualThreadController =
            TestVirtualThreadController()
    }

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
        replacementExecutor shouldNotBe closedExecutor
    }

    @Test
    fun `spring context shutdown closes controller executor`() {
        val executor = AbstractVirtualThreadController.virtualThreadExecutor

        contextRunner.run { context ->
            context.getBean<TestVirtualThreadController>().shouldNotBeNull()
            executor.isShutdown.shouldBeFalse()
        }

        executor.isShutdown.shouldBeTrue()
    }

}
