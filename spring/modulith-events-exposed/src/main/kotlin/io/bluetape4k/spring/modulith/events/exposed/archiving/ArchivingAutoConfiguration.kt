package io.bluetape4k.spring.modulith.events.exposed.archiving

import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.modulith.events.config.EventPublicationAutoConfiguration
import org.springframework.modulith.events.support.CompletionMode

@ConditionalOnProperty(name = [CompletionMode.PROPERTY], havingValue = "archive")
@AutoConfiguration
@AutoConfigureBefore(ExposedAutoConfiguration::class, EventPublicationAutoConfiguration::class)
@AutoConfigurationPackage
class ArchivingAutoConfiguration {
}
