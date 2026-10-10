package io.bluetape4k.spring.tests

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.atomic.AtomicInteger

class ServiceBean

@AutoConfiguration
class SampleAutoConfiguration {
    @Bean
    fun autoConfiguredService() = ServiceBean()
}

@Configuration(proxyBeanMethods = false)
class SampleRunnerConfiguration {
    @Bean
    fun configuredService() = ServiceBean()
}

class ApplicationContextRunnerExtensionsTest {
    @Test
    fun `auto configurations helper uses one reified configuration class`() {
        ApplicationContextRunner()
            .withConfiguration(autoConfigurationsOf<SampleAutoConfiguration>())
            .run { context ->
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `all runner wrappers keep their concrete return types`() {
        val application: ApplicationContextRunner = ApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val applicationWithNullName: ApplicationContextRunner = ApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        val web: WebApplicationContextRunner = WebApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val webWithNullName: WebApplicationContextRunner = WebApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        val reactive: ReactiveWebApplicationContextRunner = ReactiveWebApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val reactiveWithNullName: ReactiveWebApplicationContextRunner = ReactiveWebApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        application.shouldNotBeNull()
        applicationWithNullName.shouldNotBeNull()
        web.shouldNotBeNull()
        webWithNullName.shouldNotBeNull()
        reactive.shouldNotBeNull()
        reactiveWithNullName.shouldNotBeNull()
    }

    @Test
    fun `supplier and customizer run at Spring lifecycle boundaries`() {
        val eagerCalls = AtomicInteger()
        val eagerRunner = ApplicationContextRunner()
            .withBean<ServiceBean> { eagerCalls.incrementAndGet(); ServiceBean() }
        eagerCalls.get() shouldBeEqualTo 0
        eagerRunner
            .run { context ->
                eagerCalls.get() shouldBeEqualTo 1
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }

        val lazyCalls = AtomicInteger()
        val customizerCalls = AtomicInteger()
        val lazyCustomizer = BeanDefinitionCustomizer {
            customizerCalls.incrementAndGet()
            it.setLazyInit(true)
        }
        val lazyRunner = ApplicationContextRunner().withBean<ServiceBean>(lazyCustomizer) {
            lazyCalls.incrementAndGet()
            ServiceBean()
        }
        lazyCalls.get() shouldBeEqualTo 0
        customizerCalls.get() shouldBeEqualTo 0
        lazyRunner.run { context ->
            customizerCalls.get() shouldBeEqualTo 1
            lazyCalls.get() shouldBeEqualTo 0
            context.getBean(ServiceBean::class.java).shouldNotBeNull()
            lazyCalls.get() shouldBeEqualTo 1
        }

        val preparedCalls = AtomicInteger()
        ApplicationContextRunner().withBean<ServiceBean> {
            preparedCalls.incrementAndGet()
            ServiceBean()
        }.prepare { context ->
            preparedCalls.get() shouldBeEqualTo 0
            context.isActive.shouldBeFalse()
            context.sourceApplicationContext.refresh()
            context.isActive.shouldBeTrue()
            preparedCalls.get() shouldBeEqualTo 1
            context.getBean(ServiceBean::class.java).shouldNotBeNull()
        }
    }

    @Test
    fun `failures remain observable at Spring creation boundaries`() {
        val supplierFailure = IllegalStateException("supplier-failed")
        ApplicationContextRunner().withBean<ServiceBean> { throw supplierFailure }.run { context ->
            val startupFailure = context.getStartupFailure()
            (startupFailure is BeanCreationException).shouldBeTrue()
            generateSequence(startupFailure) { it.cause }.any { it === supplierFailure }.shouldBeTrue()
        }

        val lazyFailure = IllegalStateException("lazy-supplier-failed")
        val lazyCustomizer = BeanDefinitionCustomizer { it.setLazyInit(true) }
        ApplicationContextRunner().withBean<ServiceBean>(lazyCustomizer) { throw lazyFailure }.run { context ->
            context.getStartupFailure().shouldBeNull()
            val actual: Throwable? = assertFailsWith<BeanCreationException> { context.getBean(ServiceBean::class.java) }
            generateSequence(actual) { it.cause }.any { it === lazyFailure }.shouldBeTrue()
        }

        val customizerFailure = IllegalStateException("customizer-failed")
        val supplierCalls = AtomicInteger()
        ApplicationContextRunner()
            .withBean<ServiceBean>(BeanDefinitionCustomizer { throw customizerFailure }) {
                supplierCalls.incrementAndGet()
                ServiceBean()
            }
            .run { context ->
                context.getStartupFailure().shouldNotBeNull()
                context.getStartupFailure() shouldBeSameInstanceAs customizerFailure
                supplierCalls.get() shouldBeEqualTo 0
            }
    }

    @Test
    fun `repeated runs use independent contexts and derived runners do not mutate source`() {
        val calls = AtomicInteger()
        val runner = ApplicationContextRunner().withBean<ServiceBean> {
            calls.incrementAndGet()
            ServiceBean()
        }
        val contexts = mutableListOf<Any>()
        repeat(2) {
            runner.run { context ->
                contexts += context.sourceApplicationContext
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }
        }
        contexts[1].shouldNotBeSameInstanceAs(contexts[0])
        calls.get() shouldBeEqualTo 2

        val original = ApplicationContextRunner()
        val derived = original.withBean<ServiceBean> { ServiceBean() }
        original.run { context ->
            assertFailsWith<NoSuchBeanDefinitionException> { context.getBean(ServiceBean::class.java) }
        }
        derived.run { context -> context.getBean(ServiceBean::class.java).shouldNotBeNull() }
    }
}
