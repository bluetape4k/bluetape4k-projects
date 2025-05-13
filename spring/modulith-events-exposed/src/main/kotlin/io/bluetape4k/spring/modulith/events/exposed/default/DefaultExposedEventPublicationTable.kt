package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntity
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityClass
import io.bluetape4k.exposed.dao.id.TimebasedUUIDEntityID
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

@Suppress("ExposedReference")
object DefaultExposedEventPublicationTable: ExposedEventPublicationTable("event_publication") {

    val byCompletionDate = index("event_publication_by_completion_date_idx")
    val bySerializedEvent = index(
        customIndexName = "event_publication_serialized_event_idx",
        isUnique = false,
        serializedEvent,
        indexType = "HASH"
    )
}

class DefaultExposedEventPublication(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
    companion object: TimebasedUUIDEntityClass<DefaultExposedEventPublication>(DefaultExposedEventPublicationTable) {
        fun new(
            id: UUID,
            listenerId: String,
            eventType: Class<*>,
            serializedEvent: String,
            publicationDate: java.time.Instant,
            completionDate: java.time.Instant? = null,
        ): DefaultExposedEventPublication {
            return new(id) {
                this.listenerId = listenerId
                this.eventType = eventType.name
                this.serializedEvent = serializedEvent
                this.publicationDate = publicationDate
                this.completionDate = completionDate
            }
        }
    }

    var listenerId by DefaultExposedEventPublicationTable.listenerId
    var eventType by DefaultExposedEventPublicationTable.eventType
    var serializedEvent by DefaultExposedEventPublicationTable.serializedEvent
    var publicationDate by DefaultExposedEventPublicationTable.publicationDate
    var completionDate by DefaultExposedEventPublicationTable.completionDate

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

fun ResultRow.toDefaultExposedEventPublication(): ExposedEventPublication {
    return ExposedEventPublication(
        id = this[DefaultExposedEventPublicationTable.id].value,
        listenerId = this[DefaultExposedEventPublicationTable.listenerId],
        eventType = Class.forName(this[DefaultExposedEventPublicationTable.eventType]),
        serializedEvent = this[DefaultExposedEventPublicationTable.serializedEvent],
        publicationDate = this[DefaultExposedEventPublicationTable.publicationDate],
        completionDate = this[DefaultExposedEventPublicationTable.completionDate]
    )
}

fun DefaultExposedEventPublication.toExposedEventPublication(): ExposedEventPublication {
    return ExposedEventPublication(
        id = this.id.value,
        listenerId = this.listenerId,
        eventType = Class.forName(this.eventType),
        serializedEvent = this.serializedEvent,
        publicationDate = this.publicationDate,
        completionDate = this.completionDate
    )
}

fun ExposedEventPublication.toDefaultEntity(): DefaultExposedEventPublication {
    return DefaultExposedEventPublication.new(
        id = this.id,
        listenerId = this.listenerId,
        eventType = this.eventType,
        serializedEvent = this.serializedEvent,
        publicationDate = this.publicationDate,
        completionDate = this.completionDate
    )
}
