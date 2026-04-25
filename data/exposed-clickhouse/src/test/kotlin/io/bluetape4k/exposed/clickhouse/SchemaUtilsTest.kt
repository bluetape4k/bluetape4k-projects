package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.exposed.clickhouse.domain.Events
import io.bluetape4k.exposed.clickhouse.engine.mergeTree
import io.bluetape4k.exposed.clickhouse.types.ClickHouseInt32ColumnType
import io.bluetape4k.exposed.clickhouse.types.chNullable
import io.bluetape4k.exposed.clickhouse.types.lowCardinalityString
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * ClickHouse SchemaUtils DDL 생성 및 Filter 검증 테스트.
 *
 * ClickHouseTable.createStatement()가:
 * 1. CREATE TABLE 구문만 반환 (ALTER/SEQUENCE/COMMENT 제거)
 * 2. PRIMARY KEY / CONSTRAINT / REFERENCES / NOT NULL / NULL 제거
 * 3. ENGINE 절 부착
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaUtilsTest : AbstractClickHouseTest() {

    companion object : KLogging()

    @BeforeEach
    fun setup() {
        transaction(db) { SchemaUtils.create(Events) }
    }

    @AfterEach
    fun teardown() {
        transaction(db) { runCatching { SchemaUtils.drop(Events) } }
    }

    @Test
    fun `Events createStatement includes ENGINE`() {
        val statements = transaction(db) { Events.createStatement() }
        // CREATE TABLE만 있어야 함 (ALTER/SEQUENCE 없음)
        statements.size shouldBeEqualTo 1
        val ddl = statements.first()
        ddl shouldContain "CREATE TABLE"
        ddl shouldContain "ENGINE = MergeTree()"
        ddl shouldContain "ORDER BY"
    }

    @Test
    fun `Events createStatement does not include PRIMARY KEY constraint`() {
        val ddl = transaction(db) { Events.createStatement().first() }
        // CONSTRAINT pk PRIMARY KEY (...) 형태 없어야 함
        ddl.lowercase() shouldNotContain "primary key"
    }

    @Test
    fun `Events createStatement does not include NOT NULL or NULL`() {
        val ddl = transaction(db) { Events.createStatement().first() }
        ddl shouldNotContain "NOT NULL"
        // NULL word boundary (Nullable(T) 타입 내부의 NULL 제외)
        val hasStandaloneNull = ddl.contains(Regex("\\bNULL\\b"))
        hasStandaloneNull.shouldBeFalse()
    }

    @Test
    fun `Events createStatement does not include REFERENCES`() {
        val ddl = transaction(db) { Events.createStatement().first() }
        ddl.lowercase() shouldNotContain "references"
    }

    @Test
    fun `SchemaUtils create and drop Events`() {
        // setup에서 create됨 → drop 후 재생성
        transaction(db) {
            SchemaUtils.drop(Events)
            SchemaUtils.create(Events)
        }
        // 재생성 후 SELECT count (raw SQL — ClickHouse JDBC 드라이버 호환)
        val count = transaction(db) {
            exec("SELECT count(*) FROM events") { rs -> rs.next(); rs.getLong(1) } ?: 0L
        }
        count shouldBeEqualTo 0L
    }

    @Test
    fun `Table with Nullable column DDL is correct`() {
        val testTable = object : ClickHouseTable("nullable_test", mergeTree { orderBy("id") }) {
            val id = long("id")
            val nullableVal = chNullable("nullable_val", ClickHouseInt32ColumnType())
        }
        val ddl = transaction(db) { testTable.createStatement().first() }
        ddl shouldContain "Nullable(Int32)"
        ddl shouldNotContain "NOT NULL"
        val hasStandaloneNull = ddl.contains(Regex("\\bNULL\\b"))
        hasStandaloneNull.shouldBeFalse()
    }

    @Test
    fun `LowCardinality column DDL is correct`() {
        val testTable = object : ClickHouseTable("lc_test", mergeTree { orderBy("id") }) {
            val id = long("id")
            val category = lowCardinalityString("category")
        }
        val ddl = transaction(db) { testTable.createStatement().first() }
        ddl shouldContain "LowCardinality(String)"
        ddl shouldContain "ENGINE = MergeTree()"
    }
}
