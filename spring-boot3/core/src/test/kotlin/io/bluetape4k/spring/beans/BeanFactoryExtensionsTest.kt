package io.bluetape4k.spring.beans

import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class BeanFactoryExtensionsTest: AbstractSpringTest() {

    interface SampleService {
        fun greet(): String
    }

    class SampleServiceImpl: SampleService {
        override fun greet() = "hello"
    }

    @Configuration
    open class TestConfig {
        @Bean
        open fun sampleService(): SampleService = SampleServiceImpl()
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
        val service = beanFactory.get<SampleService>()
        service.shouldNotBeNull()
        service.greet() shouldBeEqualTo "hello"
    }

    @Test
    fun `get String 이름으로 빈 조회`() {
        val service: SampleService? = beanFactory["sampleService"]
        service.shouldNotBeNull()
    }

    @Test
    fun `get KClass로 빈 조회`() {
        val service = beanFactory[SampleService::class]
        service.shouldNotBeNull()
    }

    @Test
    fun `get Class로 빈 조회`() {
        val service = beanFactory[SampleService::class.java]
        service.shouldNotBeNull()
    }

    @Test
    fun `get 이름과 타입으로 빈 조회`() {
        val service = beanFactory["sampleService", SampleService::class.java]
        service.shouldNotBeNull()
    }

    @Test
    fun `get 이름과 args로 빈 조회 - args 없으면 이름만으로 조회`() {
        val service: SampleService? = beanFactory["sampleService"]
        service.shouldNotBeNull()
    }

    @Test
    fun `findBean KClass 성공`() {
        val service = beanFactory.findBean(SampleService::class)
        service.shouldNotBeNull()
    }

    @Test
    fun `findBean Class 성공`() {
        val service = beanFactory.findBean(SampleService::class.java)
        service.shouldNotBeNull()
    }

    @Test
    fun `findBean Class 없는 타입 null 반환`() {
        val result = beanFactory.findBean(String::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `findBean 이름과 타입으로 성공`() {
        val service = beanFactory.findBean("sampleService", SampleService::class.java)
        service.shouldNotBeNull()
    }

    @Test
    fun `findBean 없는 이름은 null 반환`() {
        val result = beanFactory.findBean("nonExistent", SampleService::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `findBean args 버전 성공`() {
        val result = beanFactory.findBean<Any>("sampleService")
        result.shouldNotBeNull()
    }

    @Test
    fun `findBean args 버전 없는 빈 null 반환`() {
        val result = beanFactory.findBean<Any>("nonExistentBean")
        result.shouldBeNull()
    }
}
