package io.bluetape4k.spring.beans

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.spring.AbstractSpringTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import java.util.function.Supplier

class BeanFactoryExtensionsTest: AbstractSpringTest() {

    interface SampleService {
        fun greet(): String
    }

    class SampleServiceImpl: SampleService {
        override fun greet() = "hello"
    }

    class AlternateSampleService: SampleService {
        override fun greet() = "alternate"
    }

    class FailingConstructorBean(value: String) {
        init {
            error("Cannot create bean for $value")
        }
    }

    @Configuration
    class TestConfig {
        @Bean
        fun sampleService(): SampleService = SampleServiceImpl()
    }

    @Configuration
    class DuplicateBeanConfig {
        @Bean
        fun firstSampleService(): SampleService = SampleServiceImpl()

        @Bean
        fun secondSampleService(): SampleService = AlternateSampleService()
    }

    private lateinit var context: AnnotationConfigApplicationContext
    private lateinit var beanFactory: BeanFactory

    @BeforeEach
    fun setUp() {
        context = AnnotationConfigApplicationContext(TestConfig::class.java)
        beanFactory = context
    }

    @AfterEach
    fun tearDown() {
        context.close()
    }

    @Test
    fun `get 제네릭 타입으로 빈 조회`() {
        val service = beanFactory.get<SampleService>().shouldBeInstanceOf<SampleService>()
        service.greet() shouldBeEqualTo "hello"
    }

    @Test
    fun `get String 이름으로 빈 조회`() {
        beanFactory.get<SampleService>("sampleService").shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `get KClass로 빈 조회`() {
        beanFactory[SampleService::class].shouldBeInstanceOf<SampleService>()
        beanFactory.get<SampleService>().shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `get Class로 빈 조회`() {
        beanFactory[SampleService::class.java].shouldBeInstanceOf<SampleService>()
        beanFactory.get<SampleService>().shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `get 이름과 타입으로 빈 조회`() {
        beanFactory["sampleService", SampleService::class.java].shouldBeInstanceOf<SampleService>()
        beanFactory.get<SampleService>("sampleService").shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `get 이름과 args로 빈 조회 - args 없으면 이름만으로 조회`() {
        val service: SampleService = beanFactory["sampleService"]
        service.shouldNotBeNull()
    }

    @Test
    fun `findBean KClass 성공`() {
        beanFactory.findBean(SampleService::class).shouldBeInstanceOf<SampleService>()
        beanFactory.findBean<SampleService>().shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `findBean Class 성공`() {
        beanFactory.findBean(SampleService::class.java).shouldBeInstanceOf<SampleService>()

    }

    @Test
    fun `findBean Class 없는 타입 null 반환`() {
        beanFactory.findBean(String::class.java).shouldBeNull()
    }

    @Test
    fun `findBean Class bean creation failure is rethrown`() {
        failingBeanContext().use { failingContext ->
            assertFailsWith<BeanCreationException> {
                failingContext.findBean(SampleService::class.java)
            }
        }
    }

    @Test
    fun `findBean Class duplicate beans are rethrown`() {
        AnnotationConfigApplicationContext(DuplicateBeanConfig::class.java).use { duplicateContext ->
            assertFailsWith<NoUniqueBeanDefinitionException> {
                duplicateContext.findBean(SampleService::class.java)
            }
        }
    }

    @Test
    fun `findBean 이름과 타입으로 성공`() {
        beanFactory.findBean("sampleService", SampleService::class.java).shouldBeInstanceOf<SampleService>()
        beanFactory.findBean<SampleService>("sampleService").shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `findBean 없는 이름은 null 반환`() {
        beanFactory.findBean("nonExistent", SampleService::class.java).shouldBeNull()
    }

    @Test
    fun `findBean 이름과 타입 bean creation failure is rethrown`() {
        failingBeanContext().use { failingContext ->
            assertFailsWith<BeanCreationException> {
                failingContext.findBean("brokenService", SampleService::class.java)
            }
        }
    }

    @Test
    fun `findBean args 버전 성공`() {
        beanFactory.findBean<Any>("sampleService").shouldBeInstanceOf<SampleService>()
    }

    @Test
    fun `findBean args 버전 없는 빈 null 반환`() {
        beanFactory.findBean<Any>("nonExistentBean").shouldBeNull()
    }

    @Test
    fun `findBean args 버전 bean creation failure is rethrown`() {
        failingArgsContext().use { failingContext ->
            assertFailsWith<BeanCreationException> {
                failingContext.findBean<Any>("failingConstructorBean", "boom")
            }
        }
    }

    private fun failingBeanContext(): GenericApplicationContext =
        GenericApplicationContext().apply {
            val beanDefinition =
                RootBeanDefinition(SampleService::class.java).apply {
                    instanceSupplier = Supplier<SampleService> { error("Cannot create sample service") }
                    isLazyInit = true
                }

            registerBeanDefinition("brokenService", beanDefinition)
            refresh()
        }

    private fun failingArgsContext(): GenericApplicationContext =
        GenericApplicationContext().apply {
            val beanDefinition =
                RootBeanDefinition(FailingConstructorBean::class.java).apply {
                    scope = BeanDefinition.SCOPE_PROTOTYPE
                    isLazyInit = true
                }

            registerBeanDefinition("failingConstructorBean", beanDefinition)
            refresh()
        }
}
