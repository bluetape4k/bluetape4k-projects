package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.spring.modulith.events.exposed.schema.ArchivedExposedEventPublicationTable
import io.bluetape4k.spring.modulith.events.exposed.schema.DefaultExposedEventPublicationTable
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.InitializingBean

class DatabaseSchemaInitializer(private val settings: ExposedRepositorySettings): InitializingBean {

    companion object: KLogging()

    override fun afterPropertiesSet() {
        transaction {
            val useSchema = settings.schema?.isNotBlank() == true
            if (useSchema) {
                log.info { "Creating schema ${settings.schema}..." }
                val schema = Schema(settings.schema)
                SchemaUtils.createSchema(schema)
            }

            log.info { "Creating tables. [event_publication, event_publication_archive]" }
            SchemaUtils.create(DefaultExposedEventPublicationTable, ArchivedExposedEventPublicationTable)
        }
    }
}
