package io.bluetape4k.exposed.trino.domain

import io.bluetape4k.exposed.trino.TrinoTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * 이벤트 테이블 정의 (Trino 테스트용).
 *
 * [TrinoTable]을 상속하여 Trino 비호환 DDL 구문(PRIMARY KEY, 명시적 NULL)을 자동 제거합니다.
 */
object Events : TrinoTable("events") {
    val eventId = long("event_id")
    val eventName = varchar("event_name", 255)
    val region = varchar("region", 50)
    val createdAt = timestamp("created_at").nullable()
    override val primaryKey = PrimaryKey(eventId)
}
