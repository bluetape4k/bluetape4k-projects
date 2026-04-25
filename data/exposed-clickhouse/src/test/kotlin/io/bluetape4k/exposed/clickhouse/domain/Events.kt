package io.bluetape4k.exposed.clickhouse.domain

import io.bluetape4k.exposed.clickhouse.ClickHouseTable
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.exposed.clickhouse.types.DateTime64ColumnType
import io.bluetape4k.exposed.clickhouse.types.chNullable

/**
 * 이벤트 테이블 정의 (테스트용 ClickHouseTable).
 * MergeTree ENGINE으로 생성되며, event_id, event_name 기준으로 정렬됩니다.
 *
 * created_at은 ClickHouse DateTime64(3, 'UTC') Nullable 타입으로 정의.
 * Exposed 표준 timestamp()는 ClickHouse DateTime(초 단위)로 매핑되어 밀리초 이하 정밀도가 손실됨.
 */
object Events: ClickHouseTable(
    name = "events",
    engine = mergeTree {
        orderBy("event_id", "event_name")
        partitionBy("toYYYYMM(assumeNotNull(created_at))")
    }
) {
    val eventId = long("event_id")
    val eventName = varchar("event_name", 255)
    val region = varchar("region", 50)
    val createdAt = chNullable("created_at", DateTime64ColumnType(3))
    override val primaryKey = PrimaryKey(eventId)
}
