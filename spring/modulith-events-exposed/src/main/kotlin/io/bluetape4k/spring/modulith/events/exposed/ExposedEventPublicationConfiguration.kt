package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.logging.KLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.modulith.events.config.EventPublicationConfigurationExtension
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.support.CompletionMode

@Configuration(proxyBeanMethods = false)
class ExposedEventPublicationConfiguration: EventPublicationConfigurationExtension {

    companion object: KLogging()

    @Bean
    fun exposedEventPublicationRepository(
        serializer: EventSerializer,
        environment: Environment,
    ): ExposedEventPublicationRepository {
        return ExposedEventPublicationRepository(
            serializer,
            CompletionMode.from(environment)
        )
    }
}
