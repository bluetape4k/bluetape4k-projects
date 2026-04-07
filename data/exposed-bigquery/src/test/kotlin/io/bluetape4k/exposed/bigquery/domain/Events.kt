package io.bluetape4k.exposed.bigquery.domain

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Events: Table("events") {
    val eventId = long("event_id")
    val userId = long("user_id")
    val eventType = varchar("event_type", 255)
    val region = varchar("region", 255)
    val amount = decimal("amount", 10, 2).nullable()
    val occurredAt = timestamp("occurred_at")
}
