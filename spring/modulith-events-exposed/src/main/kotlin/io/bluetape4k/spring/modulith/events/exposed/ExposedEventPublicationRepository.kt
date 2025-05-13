package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.modulith.events.core.EventPublicationRepository
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.core.PublicationTargetIdentifier
import org.springframework.modulith.events.core.TargetEventPublication
import org.springframework.modulith.events.support.CompletionMode
import java.time.Instant
import java.util.*

class ExposedEventPublicationRepository(
    private val serializer: EventSerializer,
    private val completionMode: CompletionMode,
): EventPublicationRepository {

    companion object: KLogging() {
        private const val BATCH_SIZE = 1000
    }

    val table: DefaultExposedEventPublicationTable = DefaultExposedEventPublicationTable

    private fun queryByEventAndListenerID(serializedEvent: String, listenerId: String): Query {
        return table.selectAll()
            .andWhere { table.serializedEvent eq serializedEvent }
            .andWhere { table.listenerId eq listenerId }
            .andWhere { table.completionDate.isNull() }
    }

    private fun queryByCompleteIsNotNull(): Query {
        return table.selectAll()
            .andWhere { table.completionDate.isNotNull() }
            .orderBy(table.publicationDate, SortOrder.ASC)
    }

    private fun queryByIncomplete(): Query {
        return table.selectAll()
            .andWhere { table.completionDate.isNull() }
            .orderBy(table.publicationDate, SortOrder.ASC)
    }

    private fun queryIncompleteBefore(publicationDate: Instant): Query {
        return table.selectAll()
            .andWhere { table.completionDate.isNull() }
            .andWhere { table.publicationDate less publicationDate }
            .orderBy(table.publicationDate, SortOrder.ASC)
    }

    private fun markCompletedByEventAndListenerId(
        serializedEvent: String, listenerId: String, completionDate: Instant,
    ): Int {
        return table.update({ table.completionDate.isNull() }) {
            it[table.serializedEvent] = serializedEvent
            it[table.listenerId] = listenerId
            it[table.completionDate] = completionDate
        }
    }

    private fun markCompletedById(id: UUID, completionDate: Instant): Int {
        return table.update({ table.id eq id }) {
            it[table.completionDate] = completionDate
        }
    }

    private fun deleteById(id: UUID): Int {
        return table.deleteWhere { table.id eq id }
    }

    private fun deleteByEventAndListenerId(serializedEvent: String, listenerId: String): Int {
        return table
            .deleteWhere {
                (table.serializedEvent eq serializedEvent) and (table.listenerId eq listenerId)
            }
    }

    private fun deleteCompleted(): Int {
        return table
            .deleteWhere {
                table.completionDate.isNotNull()
            }
    }

    private fun deleteCompletedBefore(completionDate: Instant): Int {
        return table
            .deleteWhere {
                table.publicationDate less publicationDate
            }
    }

    private fun serializeEvent(event: Any): String = serializer.serialize(event).toString()


    fun ExposedEventPublication.toTargetEventPublication(): TargetEventPublication {
        return ExposedEventPublicationAdapter(
            publication = this,
            serializer = serializer,
        )
    }

    fun TargetEventPublication.toExposedEventPublication(): ExposedEventPublication {
        return ExposedEventPublication(
            id = this.identifier,
            listenerId = this.targetIdentifier.value,
            eventType = this.event.javaClass,
            serializedEvent = serializeEvent(this.event),
            publicationDate = this.publicationDate,
            completionDate = this.completionDate.orElse(null),
        )
    }

    override fun create(publication: TargetEventPublication): TargetEventPublication {
        val entity = publication.toExposedEventPublication()
        transaction {
            entity.toDefaultEntity()
        }
        return publication
    }

    override fun markCompleted(
        event: Any,
        identifier: PublicationTargetIdentifier,
        completionDate: Instant,
    ) {
        TODO("Not yet implemented")
    }

    override fun markCompleted(identifier: UUID, completionDate: Instant) {
        TODO("Not yet implemented")
    }

    override fun findIncompletePublications(): List<TargetEventPublication?> {
        TODO("Not yet implemented")
    }

    override fun findIncompletePublicationsPublishedBefore(instant: Instant): List<TargetEventPublication?> {
        TODO("Not yet implemented")
    }

    override fun findIncompletePublicationsByEventAndTargetIdentifier(
        event: Any,
        targetIdentifier: PublicationTargetIdentifier,
    ): Optional<TargetEventPublication?> {
        TODO("Not yet implemented")
    }

    override fun deletePublications(identifiers: List<UUID?>) {
        TODO("Not yet implemented")
    }

    override fun deleteCompletedPublications() {
        TODO("Not yet implemented")
    }

    override fun deleteCompletedPublicationsBefore(instant: Instant) {
        TODO("Not yet implemented")
    }


    class ExposedEventPublicationAdapter(
        private val publication: ExposedEventPublication,
        private val serializer: EventSerializer,
    ): TargetEventPublication {

        private val deserializedEvent: Any by lazy {
            serializer.deserialize(publication.serializedEvent, publication.eventType)
        }

        override fun getTargetIdentifier(): PublicationTargetIdentifier =
            PublicationTargetIdentifier.of(publication.listenerId)

        override fun getIdentifier(): UUID = publication.id

        override fun getEvent(): Any = deserializedEvent

        override fun getPublicationDate(): Instant =
            publication.publicationDate

        override fun getCompletionDate(): Optional<Instant> =
            Optional.ofNullable(publication.completionDate)

        override fun isCompleted(): Boolean = publication.completionDate != null

        override fun isPublicationCompleted(): Boolean = publication.completionDate != null

        override fun markCompleted(instant: Instant) {
            publication.completionDate = instant
        }

        override fun equals(other: Any?): Boolean =
            other is ExposedEventPublicationAdapter &&
                    publication == other.publication &&
                    serializer == other.serializer

        override fun hashCode(): Int = Objects.hash(publication, serializer)
    }
}
