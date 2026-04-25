package io.bluetape4k.exposed.clickhouse.domain

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * 이벤트 테이블 정의 (테스트용).
 *
 * T7에서 ClickHouseTable + MergeTree 엔진 DSL이 구현된 후 교체될 예정입니다.
 * T3 단계에서는 컴파일 의존성을 만족시키기 위한 단순 Table 정의만 제공합니다.
 */
object Events: Table("events") {
    val eventId = long("event_id")
    val eventName = varchar("event_name", 255)
    val region = varchar("region", 50)
    val createdAt = timestamp("created_at").nullable()
    override val primaryKey = PrimaryKey(eventId)
}
