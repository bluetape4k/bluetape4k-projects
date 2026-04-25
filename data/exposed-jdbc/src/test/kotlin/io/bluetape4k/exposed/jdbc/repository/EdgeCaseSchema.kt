package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId

/**
 * 엣지 케이스 테스트용 공유 스키마 정의.
 *
 * T-EJ-01 (ExistenceTest), T-EJ-02 (WriteEdgeCaseTest), T-EJ-03 (ReadEdgeCaseTest)에서 공유 사용.
 */
object EdgeCaseSchema {

    /**
     * 엣지 케이스 테스트용 테이블.
     * name 컬럼에 UNIQUE 제약이 있어 batchUpsert 충돌 시나리오 구현 가능.
     */
    object EdgeCaseTable : LongIdTable("edge_case_items") {
        val name = varchar("name", 255).uniqueIndex()
        val age = integer("age").default(0)
        val isActive = bool("is_active").default(true)
    }

    /**
     * 엣지 케이스 테이블의 레코드 타입.
     */
    data class EdgeCaseRecord(
        val id: Long = 0L,
        val name: String,
        val age: Int = 0,
        val isActive: Boolean = true,
    )

    /**
     * EdgeCaseTable에 대한 JDBC Repository 구현체.
     */
    object EdgeCaseRepository : LongJdbcRepository<EdgeCaseRecord> {
        override val table = EdgeCaseTable

        override fun extractId(entity: EdgeCaseRecord): Long = entity.id

        override fun ResultRow.toEntity(): EdgeCaseRecord = EdgeCaseRecord(
            id = this[EdgeCaseTable.id].value,
            name = this[EdgeCaseTable.name],
            age = this[EdgeCaseTable.age],
            isActive = this[EdgeCaseTable.isActive],
        )

        /**
         * 새 레코드를 삽입하고 저장된 레코드를 반환한다.
         */
        fun save(record: EdgeCaseRecord): EdgeCaseRecord {
            val id = EdgeCaseTable.insertAndGetId {
                it[name] = record.name
                it[age] = record.age
                it[isActive] = record.isActive
            }
            return record.copy(id = id.value)
        }
    }

    /**
     * EdgeCaseTable을 사용하는 트랜잭션 블록을 실행한다.
     */
    fun AbstractExposedTest.withEdgeCaseTable(
        testDB: TestDB,
        statement: JdbcTransaction.() -> Unit,
    ) {
        withTables(testDB, EdgeCaseTable) { statement() }
    }
}
