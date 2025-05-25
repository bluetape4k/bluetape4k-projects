package io.bluetape4k.spring.modulith.events.exposed

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.modulith.events.exposed")
data class ExposedConfigurationProperties(
    val schemaInitialization: SchemaInitialization = SchemaInitialization(),
    val schema: String? = null,
) {

    data class SchemaInitialization(
        val enabled: Boolean = false,
    )
}
