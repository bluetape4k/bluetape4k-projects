package io.bluetape4k.exposed.clickhouse.domain

import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * 이벤트 테이블 정의 (테스트용 ClickHouseTable).
 * MergeTree ENGINE으로 생성되며, event_id, event_name 기준으로 정렬됩니다.
 */
object Events: ClickHouseTable(
    name = "events",
    engine = mergeTree {
        orderBy("event_id", "event_name")
        partitionBy("toYYYYMM(created_at)")
    }
) {
    val eventId = long("event_id")
    val eventName = varchar("event_name", 255)
    val region = varchar("region", 50)
    val createdAt = timestamp("created_at").nullable()
    override val primaryKey = PrimaryKey(eventId)
}
