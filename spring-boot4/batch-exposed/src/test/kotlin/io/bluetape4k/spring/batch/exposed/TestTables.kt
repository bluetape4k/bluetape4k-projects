package io.bluetape4k.spring.batch.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable

/**
 * 테스트용 Source 테이블 정의.
 */
object SourceTable : LongIdTable("source_data") {
    val name = varchar("name", 255)
    val value = integer("value")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

/**
 * 테스트용 Target 테이블 정의.
 * sourceName을 PK로 사용하여 batchUpsert가 ON CONFLICT(source_name) 으로 동작하도록 함.
 */
object TargetTable : Table("target_data") {
    val sourceName = varchar("source_name", 255)
    val transformedValue = integer("transformed_value")
    override val primaryKey = PrimaryKey(sourceName)
}

/**
 * Source 테이블 레코드.
 */
data class SourceRecord(
    val id: Long = 0L,
    val name: String,
    val value: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Target 테이블 레코드.
 */
data class TargetRecord(
    val sourceName: String,
    val transformedValue: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 테스트 데이터 대량 생성 헬퍼.
 *
 * 활성 트랜잭션이 있으면 이를 재사용하고, 없으면 새 트랜잭션을 시작합니다.
 */
fun insertTestData(count: Int) {
    transaction {
        SourceTable.batchInsert((1..count).toList(), shouldReturnGeneratedValues = false) { i ->
            this[SourceTable.name] = "item-$i"
            this[SourceTable.value] = i
        }
    }
}
