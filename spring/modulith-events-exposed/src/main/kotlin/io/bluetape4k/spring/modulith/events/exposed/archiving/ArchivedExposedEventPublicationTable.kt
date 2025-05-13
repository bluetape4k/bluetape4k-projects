package io.bluetape4k.spring.modulith.events.exposed.archiving

import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityID
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.spring.modulith.events.exposed.ExposedEventPublication
import io.bluetape4k.spring.modulith.events.exposed.ExposedEventPublicationTable
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

@Suppress("ExposedReference")
object ArchivedExposedEventPublicationTable: ExposedEventPublicationTable("event_publication_archive") {

    val byCompletion = index("event_publication_archive_by_completion_date_idx")
    val bySerializedEvent = index(
        customIndexName = "event_publication_archive_serialized_event_idx",
        isUnique = false,
        serializedEvent,
        indexType = "HASH"
    )
}

class ArchivedExposedEventPublication(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
    companion object: TimebasedUUIDEntityClass<ArchivedExposedEventPublication>(ArchivedExposedEventPublicationTable) {
        fun new(
            id: UUID,
            listenerId: String,
            eventType: Class<*>,
            serializedEvent: String,
            publicationDate: java.time.Instant,
            completionDate: java.time.Instant? = null,
        ): ArchivedExposedEventPublication {
            return new(id) {
                this.listenerId = listenerId
                this.eventType = eventType.name
                this.serializedEvent = serializedEvent
                this.publicationDate = publicationDate
                this.completionDate = completionDate
            }
        }
    }

    var listenerId by ArchivedExposedEventPublicationTable.listenerId
    var eventType by ArchivedExposedEventPublicationTable.eventType
    var serializedEvent by ArchivedExposedEventPublicationTable.serializedEvent
    var publicationDate by ArchivedExposedEventPublicationTable.publicationDate
    var completionDate by ArchivedExposedEventPublicationTable.completionDate

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("listenerId", listenerId)
        .add("eventType", eventType)
        .add("serializedEvent", serializedEvent)
        .add("publicationDate", publicationDate)
        .add("completionDate", completionDate)
        .toString()
}

fun ResultRow.toArchiveToExposedEventPublication(): ExposedEventPublication {
    return ExposedEventPublication(
        id = this[ArchivedExposedEventPublicationTable.id].value,
        listenerId = this[ArchivedExposedEventPublicationTable.listenerId],
        eventType = Class.forName(this[ArchivedExposedEventPublicationTable.eventType]),
        serializedEvent = this[ArchivedExposedEventPublicationTable.serializedEvent],
        publicationDate = this[ArchivedExposedEventPublicationTable.publicationDate],
        completionDate = this[ArchivedExposedEventPublicationTable.completionDate]
    )
}

fun ArchivedExposedEventPublication.toExposedEventPublication(): ExposedEventPublication {
    return ExposedEventPublication(
        id = id.value,
        listenerId = listenerId,
        eventType = Class.forName(eventType),
        serializedEvent = serializedEvent,
        publicationDate = publicationDate,
        completionDate = completionDate
    )
}

fun ExposedEventPublication.toArchivedExposedEventPublication(): ArchivedExposedEventPublication {
    return ArchivedExposedEventPublication.new(
        id = id,
        listenerId = listenerId,
        eventType = eventType,
        serializedEvent = serializedEvent,
        publicationDate = publicationDate,
        completionDate = completionDate
    )
}
