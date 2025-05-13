package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.exposed.dao.HasIdentifier
import io.bluetape4k.exposed.dao.id.TimebasedUUIDTable
import io.bluetape4k.spring.modulith.events.exposed.archiving.ArchivedExposedEventPublication
import org.jetbrains.exposed.sql.javatime.timestamp
import org.springframework.modulith.events.support.CompletionMode
import java.time.Instant
import java.util.*

@Suppress("ExposedReference")
open class ExposedEventPublicationTable(name: String = ""): TimebasedUUIDTable(name) {
    val listenerId = text("listener_id")
    val eventType = text("event_type")
    val serializedEvent = text("serialized_event")
    val publicationDate = timestamp("publication_date")
    val completionDate = timestamp("completion_date").nullable()
}

/**
 * Spring Modulith 에서 Events 를 Exposed 를 이용하여 DB에 저장하기 위한 엔티티입니다.
 */
data class ExposedEventPublication(
    override val id: UUID,
    val listenerId: String,
    val eventType: Class<*>,
    val serializedEvent: String,
    var publicationDate: Instant,
    var completionDate: Instant? = null,
): HasIdentifier<UUID> {

    companion object {

        fun getIncompletedType(): Class<*> = DefaultExposedEventPublication::class.java

        fun getCompletedType(mode: CompletionMode): Class<*> = when (mode) {
            CompletionMode.ARCHIVE -> ArchivedExposedEventPublication::class.java
            else -> DefaultExposedEventPublication::class.java
        }
    }
}
