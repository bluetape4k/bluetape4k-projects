package io.bluetape4k.spring.modulith.events.exposed

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.modulith.events.exposed.schema.ExposedEventPublicationTable
import io.bluetape4k.support.toOptional
import org.jetbrains.exposed.sql.ResultRow
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.modulith.events.core.PublicationTargetIdentifier
import org.springframework.modulith.events.core.TargetEventPublication
import org.springframework.util.ClassUtils
import java.io.Serializable
import java.time.Instant
import java.util.*


/**
 * Spring Modulith 에서 Events 를 Exposed 를 이용하여 DB에 저장하기 위한 엔티티입니다.
 */
open class ExposedEventPublication(
    val id: UUID,
    val listenerId: String,
    val serializedEvent: String,
    val eventType: Class<*>,
    val publicationTime: Instant,
    val eventSupplier: () -> Any,
    var completionTime: Instant? = null,
): TargetEventPublication, Serializable {

    companion object: KLogging()

    private var event: Any? = null

    override fun getIdentifier(): UUID = id

    override fun getEvent(): Any {
        if (event == null) {
            event = eventSupplier()
        }
        return event!!
    }

    override fun getTargetIdentifier(): PublicationTargetIdentifier =
        PublicationTargetIdentifier.of(listenerId)

    override fun getPublicationDate(): Instant = publicationTime

    override fun getCompletionDate(): Optional<Instant> = completionTime.toOptional()

    override fun markCompleted(completionDate: Instant) {
        this.completionTime = completionDate
    }

    override fun equals(other: Any?): Boolean {
        return other is ExposedEventPublication &&
                id == other.id &&
                listenerId == other.listenerId &&
                eventType == other.eventType &&
                serializedEvent == other.serializedEvent &&
                publicationTime == other.publicationTime &&
                completionTime == other.completionTime
    }

    override fun hashCode(): Int {
        return Objects.hash(id, listenerId, eventType, serializedEvent, publicationTime, completionTime)
    }

    override fun toString(): String =
        ToStringBuilder(this)
            .add("id", id)
            .add("listenerId", listenerId)
            .add("eventType", eventType)
            .add("serializedEvent", serializedEvent)
            .add("publicationDate", publicationTime)
            .add("completionDate", completionTime)
            .toString()
}


fun ResultRow.toExposedEventPublication(
    table: ExposedEventPublicationTable,
    serializer: EventSerializer,
): ExposedEventPublication? {
    val eventClass = runCatching {
        ClassUtils.forName(this[table.eventType], Thread.currentThread().contextClassLoader)
    }.getOrNull()

    if (eventClass == null) {
        return null
    }

    val eventSupplier = {
        serializer.deserialize(
            this[table.serializedEvent],
            Class.forName(this[table.eventType])
        )
    }
    return ExposedEventPublication(
        id = this[table.id].value,
        listenerId = this[table.listenerId],
        serializedEvent = this[table.serializedEvent],
        eventType = eventClass,
        eventSupplier = eventSupplier,
        publicationTime = this[table.publicationDate],
        completionTime = this[table.completionDate]
    )
}
