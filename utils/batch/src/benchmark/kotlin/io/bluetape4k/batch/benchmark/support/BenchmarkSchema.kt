package io.bluetape4k.batch.benchmark.support

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * 벤치마크 소스 데이터를 저장하는 테이블입니다.
 */
object BenchmarkSourceTable : Table("benchmark_source") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val value = integer("value")

    override val primaryKey = PrimaryKey(id)
}

/**
 * 벤치마크 처리 결과를 저장하는 대상 테이블입니다.
 */
object BenchmarkTargetTable : LongIdTable("benchmark_target") {
    val sourceName = varchar("source_name", 255).uniqueIndex()
    val transformedValue = integer("transformed_value")
}
