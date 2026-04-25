package io.bluetape4k.exposed.clickhouse

/**
 * ClickHouse에서 미지원 기능 사용 시 명확한 오류 메시지를 제공하는 헬퍼입니다.
 */
object ClickHouseUnsupported {
    fun insertIgnore(): Nothing =
        error("ClickHouse does not support INSERT IGNORE. Use ReplacingMergeTree or DEDUPLICATE BY instead.")

    fun upsert(): Nothing =
        error("ClickHouse does not support ON CONFLICT / UPSERT. Use ReplacingMergeTree instead.")

    fun returningClause(): Nothing =
        error("ClickHouse does not support RETURNING clause.")
}
