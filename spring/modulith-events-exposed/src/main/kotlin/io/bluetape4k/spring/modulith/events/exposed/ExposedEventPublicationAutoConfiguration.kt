package io.bluetape4k.spring.modulith.events.exposed

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.modulith.events.config.EventPublicationAutoConfiguration

@AutoConfiguration
@AutoConfigureBefore(ExposedAutoConfiguration::class, EventPublicationAutoConfiguration::class)
@AutoConfigurationPackage(basePackageClasses = [ExposedEventPublication::class])
class ExposedEventPublicationAutoConfiguration {
}
