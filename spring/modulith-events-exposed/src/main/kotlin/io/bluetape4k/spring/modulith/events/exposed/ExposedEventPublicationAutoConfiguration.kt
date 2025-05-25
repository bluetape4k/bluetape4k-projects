package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.modulith.events.config.EventPublicationAutoConfiguration
import org.springframework.modulith.events.config.EventPublicationConfigurationExtension
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.support.CompletionMode

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(ExposedAutoConfiguration::class, EventPublicationAutoConfiguration::class)
@AutoConfigurationPackage(basePackageClasses = [ExposedEventPublication::class])
@EnableConfigurationProperties(ExposedConfigurationProperties::class)
class ExposedEventPublicationAutoConfiguration: EventPublicationConfigurationExtension {

    companion object: KLogging()

    @Autowired
    private val environment: Environment = uninitialized()

    @Bean
    fun exposedRepositorySettings(
        properties: ExposedConfigurationProperties,
    ): ExposedRepositorySettings {
        val schema = properties.schema
        val completionMode = CompletionMode.from(environment)

        return ExposedRepositorySettings(
            schema = schema,
            completionMode = completionMode,
        )
    }

    @Bean
    fun exposedEventPublicationRepository(
        serializer: EventSerializer,
        settings: ExposedRepositorySettings,
    ): ExposedEventPublicationRepository {
        return ExposedEventPublicationRepository(serializer, settings)
    }

    @Bean
    @ConditionalOnProperty(
        name = ["spring.modulith.events.exposed.schema-initialization.enabled"],
        havingValue = "true"
    )
    fun databaseSchemaInitializer(settings: ExposedRepositorySettings): DatabaseSchemaInitializer? {
        return DatabaseSchemaInitializer(settings)
    }
}
