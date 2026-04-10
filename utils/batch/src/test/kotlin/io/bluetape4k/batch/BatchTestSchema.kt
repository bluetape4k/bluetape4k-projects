package io.bluetape4k.batch

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * 배치 통합 테스트용 소스 테이블.
 *
 * Reader가 이 테이블에서 데이터를 읽고, Writer가 [BatchTargetTable]로 변환/저장한다.
 * keyColumn으로 사용하기 위해 [Column<Long>] 타입의 id를 명시적으로 선언한다.
 */
object BatchSourceTable : Table("batch_source") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val value = integer("value")
    override val primaryKey = PrimaryKey(id)
}

/**
 * 배치 통합 테스트용 대상 테이블.
 *
 * [BatchSourceTable]에서 읽은 데이터를 Writer가 이 테이블에 저장한다.
 */
object BatchTargetTable : LongIdTable("batch_target") {
    val sourceName = varchar("source_name", 255).uniqueIndex()
    val transformedValue = integer("transformed_value")
}

/** [BatchSourceTable] 레코드 */
data class SourceRecord(val id: Long, val name: String, val value: Int)

/** [BatchTargetTable] 레코드 */
data class TargetRecord(val sourceName: String, val transformedValue: Int)
