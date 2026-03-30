package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.BatchInsertBlockingExecutable
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BatchInsertOnConflictDoNothingTest: AbstractExposedTest() {

    companion object: KLogging()

    private object CompositeKeyTester : Table("batch_insert_composite_tester") {
        val tenantId = varchar("tenant_id", 20)
        val naturalKey = varchar("natural_key", 20)
        val payload = varchar("payload", 50)

        override val primaryKey = PrimaryKey(tenantId, naturalKey)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert number of inserted rows`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB + TestDB.ALL_POSTGRES_LIKE }

        val tester = object: Table("tester") {
            val id = varchar("id", 10).uniqueIndex()
        }
        withTables(testDB, tester) {
            tester.insert { it[id] = "foo" }

            // 중복된 id 를 가진 row 를 추가하면, 무시합니다.
            val executable = BatchInsertBlockingExecutable(statement = BatchInsertOnConflictDoNothing(tester))
            executable.run {
                statement.addBatch()
                statement[tester.id] = "foo"        // 중복되므로 insert 되지 않음

                statement.addBatch()
                statement[tester.id] = "bar"        // 중복되지 않으므로 추가됨

                execute(this@withTables)
            }
            tester.selectAll().count() shouldBeEqualTo 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `PostgreSQL 계열에서는 복합 PK를 conflict target으로 사용한다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE }

        withTables(testDB, CompositeKeyTester) {
            val statement = BatchInsertOnConflictDoNothing(CompositeKeyTester)
            statement.addBatch()
            statement[CompositeKeyTester.tenantId] = "tenant-a"
            statement[CompositeKeyTester.naturalKey] = "key-1"
            statement[CompositeKeyTester.payload] = "payload"

            val sql = statement.prepareSQL(this, prepared = true)

            sql shouldContain "ON CONFLICT"
            sql shouldContain "${identity(CompositeKeyTester.tenantId)}, ${identity(CompositeKeyTester.naturalKey)}"
            sql shouldContain "DO NOTHING"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `MySQL 계열에서는 INSERT IGNORE 를 사용한다`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withTables(testDB, CompositeKeyTester) {
            val statement = BatchInsertOnConflictDoNothing(CompositeKeyTester)
            statement.addBatch()
            statement[CompositeKeyTester.tenantId] = "tenant-a"
            statement[CompositeKeyTester.naturalKey] = "key-1"
            statement[CompositeKeyTester.payload] = "payload"

            val sql = statement.prepareSQL(this, prepared = true)

            sql shouldContain "INSERT IGNORE"
        }
    }
}
