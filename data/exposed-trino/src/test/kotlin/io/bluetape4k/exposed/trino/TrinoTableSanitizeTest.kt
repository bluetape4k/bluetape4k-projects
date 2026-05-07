package io.bluetape4k.exposed.trino

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * [TrinoTable]의 DDL sanitize 로직을 검증하는 테스트.
 *
 * `createStatement()`는 Exposed 트랜잭션 컨텍스트가 필요하므로
 * Testcontainers Trino 연결을 사용합니다.
 */
class TrinoTableSanitizeTest : AbstractTrinoTest() {

    /**
     * PrimaryKey를 가진 Trino 테이블 픽스처.
     */
    private object SimpleTable : TrinoTable("sanitize_simple_tbl") {
        val id = long("id")
        val name = varchar("name", 100)
        override val primaryKey = PrimaryKey(id)
    }

    /**
     * 표준 Exposed Table: PRIMARY KEY 구문이 DDL에 포함됩니다.
     */
    private object PlainTable : Table("sanitize_plain_tbl") {
        val id = long("id")
        val name = varchar("name", 100)
        override val primaryKey = PrimaryKey(id)
    }

    /**
     * nullable 컬럼을 포함하는 TrinoTable (NULL 키워드 제거 검증용).
     */
    private object NullableTable : TrinoTable("sanitize_nullable_tbl") {
        val id = long("id")
        val description = text("description").nullable()
    }

    /**
     * NOT NULL 컬럼만 가진 TrinoTable (NOT NULL 보존 검증용).
     */
    private object NotNullTable : TrinoTable("sanitize_notnull_tbl") {
        val id = long("id")
        val required = varchar("required", 50)
    }

    // ----------------------------------------------------------------
    // 테스트 1: TrinoTable DDL에는 PRIMARY KEY가 없어야 한다
    // ----------------------------------------------------------------

    @Test
    fun `TrinoTable createStatement 는 PRIMARY KEY 구문을 포함하지 않는다`() {
        transaction(db) {
            val ddl = SimpleTable.createStatement().single()
            ddl.contains("PRIMARY KEY", ignoreCase = true).shouldBeFalse()
        }
    }

    // ----------------------------------------------------------------
    // 테스트 2: 일반 Table은 PRIMARY KEY를 포함하는 것과 대비
    // ----------------------------------------------------------------

    @Test
    fun `일반 Table createStatement 는 PRIMARY KEY 구문을 포함한다`() {
        transaction(db) {
            val ddl = PlainTable.createStatement().single()
            ddl.contains("PRIMARY KEY", ignoreCase = true).shouldBeTrue()
        }
    }

    // ----------------------------------------------------------------
    // 테스트 3: CONSTRAINT 구문도 제거된다
    // ----------------------------------------------------------------

    @Test
    fun `TrinoTable createStatement 에는 CONSTRAINT 구문이 없다`() {
        transaction(db) {
            val ddl = SimpleTable.createStatement().single()
            ddl.contains("CONSTRAINT", ignoreCase = true).shouldBeFalse()
        }
    }

    // ----------------------------------------------------------------
    // 테스트 4: nullable 컬럼에서 명시적 NULL 키워드가 제거된다
    // ----------------------------------------------------------------

    @Test
    fun `nullable 컬럼의 명시적 NULL 키워드는 DDL에서 제거된다`() {
        transaction(db) {
            val ddl = NullableTable.createStatement().single()
            // Exposed가 생성하는 " NULL" (단독 NULL 키워드)가 없어야 함
            // NOT NULL은 보존되어야 하므로 단독 NULL만 검사
            val hasStandaloneNull = Regex("(?<!NOT)\\s+NULL\\b").containsMatchIn(ddl)
            hasStandaloneNull.shouldBeFalse()
        }
    }

    // ----------------------------------------------------------------
    // 테스트 5: NOT NULL 컬럼의 NOT NULL은 보존된다
    // ----------------------------------------------------------------

    @Test
    fun `NOT NULL 컬럼의 NOT NULL 구문은 DDL에 보존된다`() {
        transaction(db) {
            val ddl = NotNullTable.createStatement().single()
            ddl shouldContain "NOT NULL"
        }
    }

    // ----------------------------------------------------------------
    // 테스트 6: TrinoTable 이름이 DDL에 올바르게 포함된다
    // ----------------------------------------------------------------

    @Test
    fun `TrinoTable createStatement 에 테이블 이름이 포함된다`() {
        transaction(db) {
            val ddl = SimpleTable.createStatement().single()
            ddl shouldContain "sanitize_simple_tbl"
        }
    }

    // ----------------------------------------------------------------
    // 테스트 7: TrinoTable 컬럼이 DDL에 포함된다
    // ----------------------------------------------------------------

    @Test
    fun `TrinoTable createStatement 에 정의한 컬럼이 포함된다`() {
        transaction(db) {
            val ddl = SimpleTable.createStatement().single()
            ddl shouldContain "id"
            ddl shouldContain "name"
        }
    }
}
