package io.bluetape4k.spring.context.annotation

import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.AnnotationConfigRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class SampleBean

@Configuration
class SampleConfiguration {
    @Bean
    fun sampleBean() = SampleBean()
}

class AnnotationConfigRegistryExtensionsTest {
    @Test
    fun `register reified configuration`() {
        val context = AnnotationConfigApplicationContext()
        try {
            val registry: AnnotationConfigRegistry = context
            registry.register<SampleConfiguration>()
            context.refresh()
            context.getBean(SampleBean::class.java).shouldNotBeNull()
        } finally {
            context.close()
        }
    }
}
