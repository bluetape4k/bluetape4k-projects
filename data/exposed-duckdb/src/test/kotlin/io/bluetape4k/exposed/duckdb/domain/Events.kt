package io.bluetape4k.exposed.duckdb.domain

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * 이벤트 테이블 정의 (DuckDB 테스트용).
 */
object Events: Table("events") {
    val eventId = long("event_id")
    val userId = long("user_id")
    val eventType = varchar("event_type", 50)
    val region = varchar("region", 10)
    val amount = decimal("amount", 15, 2).nullable()
    val occurredAt = timestamp("occurred_at")

    override val primaryKey = PrimaryKey(eventId)
}
