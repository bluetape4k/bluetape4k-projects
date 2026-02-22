package io.bluetape4k.exposed.tests

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class WithTablesTest {

    companion object: KLogging()

    private object TestTable: IntIdTable("test_with_tables") {
        val name = varchar("name", 255)
    }

    @Nested
    inner class Jdbc: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withTables should create and drop tables`(testDB: TestDB) {
            withTables(testDB, TestTable) {
                TestTable.insert {
                    it[name] = "test"
                }
                commit()

                val count = TestTable.selectAll().count()
                count shouldBeEqualTo 1L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withTables should pass testDB to statement`(testDB: TestDB) {
            withTables(testDB, TestTable) { db ->
                db shouldBeEqualTo testDB
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withTables should keep tables when dropTables is false`(testDB: TestDB) {
            withTables(testDB, TestTable, dropTables = false) {
                TestTable.insert {
                    it[name] = "test"
                }
                commit()
            }

            withTables(testDB, TestTable, dropTables = false) {
                val count = TestTable.selectAll().count()
                count shouldBeGreaterThan 0L
            }

            withTables(testDB, TestTable) {
                val count = TestTable.selectAll().count()
                count shouldBeEqualTo 0L
            }
        }
    }

    @Nested
    inner class Coroutines: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedTables should create and drop tables`(testDB: TestDB) = runSuspendIO {
            withTablesSuspending(testDB, TestTable) {
                TestTable.insert {
                    it[name] = "test"
                }
                commit()

                val count = TestTable.selectAll().count()
                count shouldBeEqualTo 1L
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedTables should pass testDB to statement`(testDB: TestDB) = runSuspendIO {
            withTablesSuspending(testDB, TestTable) { db ->
                db shouldBeEqualTo testDB
            }
        }
    }
}
