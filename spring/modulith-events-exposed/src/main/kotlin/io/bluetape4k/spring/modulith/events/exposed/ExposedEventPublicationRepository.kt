package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.modulith.events.exposed.schema.ArchivedExposedEventPublicationTable
import io.bluetape4k.spring.modulith.events.exposed.schema.DefaultExposedEventPublicationTable
import io.bluetape4k.spring.modulith.events.exposed.schema.ExposedEventPublicationTable
import io.bluetape4k.spring.modulith.events.exposed.schema.copyToArchiveByEventAndListenerId
import io.bluetape4k.spring.modulith.events.exposed.schema.copyToArchiveById
import io.bluetape4k.spring.modulith.events.exposed.schema.deleteByEventAndListenerId
import io.bluetape4k.spring.modulith.events.exposed.schema.deleteById
import io.bluetape4k.spring.modulith.events.exposed.schema.deleteByIds
import io.bluetape4k.spring.modulith.events.exposed.schema.deleteCompleted
import io.bluetape4k.spring.modulith.events.exposed.schema.deleteCompletedBefore
import io.bluetape4k.spring.modulith.events.exposed.schema.markCompletedById
import io.bluetape4k.spring.modulith.events.exposed.schema.markCompletedByListenerIdAndEvent
import io.bluetape4k.spring.modulith.events.exposed.schema.queryByCompleteIsNotNull
import io.bluetape4k.spring.modulith.events.exposed.schema.queryByIncomplete
import io.bluetape4k.spring.modulith.events.exposed.schema.queryByListenerIdAndEvent
import io.bluetape4k.spring.modulith.events.exposed.schema.queryIncompleteBefore
import io.bluetape4k.support.toOptional
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.modulith.events.core.EventPublicationRepository
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.core.PublicationTargetIdentifier
import org.springframework.modulith.events.core.TargetEventPublication
import org.springframework.modulith.events.support.CompletionMode
import java.time.Instant
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ExposedEventPublicationRepository(
    private val serializer: EventSerializer,
    private val settings: ExposedRepositorySettings,
): EventPublicationRepository {

    companion object: KLogging() {
        private const val BATCH_SIZE = 1000
    }

    private val defaultTable: ExposedEventPublicationTable = DefaultExposedEventPublicationTable
    private val archiveTable: ExposedEventPublicationTable = ArchivedExposedEventPublicationTable


    private fun serializeEvent(event: Any): String =
        serializer.serialize(event).toString()

    fun TargetEventPublication.toExposedEventPublication(): ExposedEventPublication {
        return ExposedEventPublication(
            id = this.identifier,
            listenerId = this.targetIdentifier.value,
            eventType = this.event.javaClass,
            serializedEvent = serializeEvent(this.event),
            publicationTime = this.publicationDate,
            eventSupplier = { this.event },
            completionTime = this.completionDate.getOrNull()
        )
    }

    override fun create(publication: TargetEventPublication): TargetEventPublication {
        val exposedPublication = publication.toExposedEventPublication()
        log.debug { "Insert publication. exposed publication=$exposedPublication" }
        transaction {
            defaultTable.insert {
                it[id] = exposedPublication.id
                it[listenerId] = exposedPublication.listenerId
                it[eventType] = exposedPublication.eventType.name
                it[serializedEvent] = exposedPublication.serializedEvent
                it[publicationDate] = exposedPublication.publicationTime
            }
        }
        return publication
    }

    override fun markCompleted(
        event: Any,
        identifier: PublicationTargetIdentifier,
        completionDate: Instant,
    ) {
        val targetIdentifier = identifier.value
        val serializedEvent = serializer.serialize(event).toString()
        transaction {
            when (settings.completionMode) {
                CompletionMode.DELETE ->
                    defaultTable.deleteByEventAndListenerId(targetIdentifier, serializedEvent)
                CompletionMode.ARCHIVE -> {
                    defaultTable.copyToArchiveByEventAndListenerId(
                        archiveTable,
                        targetIdentifier,
                        serializedEvent,
                        completionDate
                    )
                    defaultTable.deleteByEventAndListenerId(targetIdentifier, serializedEvent)
                }
                CompletionMode.UPDATE ->
                    defaultTable.markCompletedByListenerIdAndEvent(
                        listenerId = targetIdentifier,
                        serializedEvent = serializedEvent,
                        completionDate = completionDate,
                    )
            }
        }
    }

    override fun markCompleted(identifier: UUID, completionDate: Instant) {
        transaction {
            when (settings.completionMode) {
                CompletionMode.DELETE ->
                    defaultTable.deleteById(identifier)
                CompletionMode.ARCHIVE -> {
                    defaultTable.copyToArchiveById(archiveTable, identifier, completionDate)
                    defaultTable.deleteById(identifier)
                }
                CompletionMode.UPDATE -> {
                    defaultTable.markCompletedById(identifier, completionDate)
                }
            }
        }
    }

    override fun findIncompletePublications(): List<TargetEventPublication?> {
        return transaction {
            defaultTable
                .queryByIncomplete()
                .map { it.toExposedEventPublication(defaultTable, serializer) }
        }
    }

    override fun findIncompletePublicationsPublishedBefore(instant: Instant): List<TargetEventPublication?> {
        return transaction {
            defaultTable
                .queryIncompleteBefore(instant)
                .map { it.toExposedEventPublication(defaultTable, serializer) }
        }
    }

    override fun findIncompletePublicationsByEventAndTargetIdentifier(
        event: Any,
        targetIdentifier: PublicationTargetIdentifier,
    ): Optional<TargetEventPublication> {
        val serializedEvent = serializer.serialize(event).toString()

        return transaction {
            defaultTable
                .queryByListenerIdAndEvent(targetIdentifier.value, serializedEvent)
                .firstOrNull()
                ?.toExposedEventPublication(defaultTable, serializer)
                .toOptional()
        }
    }

    override fun findCompletedPublications(): List<TargetEventPublication?> {
        return transaction {
            defaultTable
                .queryByCompleteIsNotNull()
                .map { it.toExposedEventPublication(defaultTable, serializer) }
        }
    }

    override fun deletePublications(identifiers: List<UUID>) {
        transaction {
            archiveTable.deleteByIds(identifiers)
        }
    }

    override fun deleteCompletedPublications() {
        transaction {
            archiveTable.deleteCompleted()
        }
    }

    override fun deleteCompletedPublicationsBefore(instant: Instant) {
        transaction {
            archiveTable.deleteCompletedBefore(instant)
        }
    }
}
