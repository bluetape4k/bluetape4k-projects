package io.bluetape4k.spring.modulith.events.exposed.schema

import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

@Suppress("ExposedReference")
open class ExposedEventPublicationTable(name: String = ""): TimebasedUUIDTable(name) {
    val listenerId = text("listener_id")
    val eventType = text("event_type")
    val serializedEvent = text("serialized_event")
    val publicationDate = timestamp("publication_date")
    val completionDate = timestamp("completion_date").nullable()
}

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
