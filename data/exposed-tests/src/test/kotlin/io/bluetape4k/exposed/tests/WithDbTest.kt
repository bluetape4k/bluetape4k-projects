package io.bluetape4k.exposed.tests

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class WithDbTest {
    companion object: KLogging()

    private object TestTable: IntIdTable("test_with_db") {
        val name = varchar("name", 255)
    }

    @Nested
    inner class Jdbc: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withDb should provide database connection`(testDB: TestDB) {
            withDb(testDB) {
                currentDialectTest.shouldNotBeNull()
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withDb should allow database operations`(testDB: TestDB) {
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
        fun `withDb should set currentTestDB`(testDB: TestDB) {
            withDb(testDB) {
                currentTestDB.shouldNotBeNull()
                currentTestDB shouldBeEqualTo testDB
            }
        }
    }

    @Nested
    inner class Coroutines: AbstractExposedTest() {
        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedDb should provide database connection`(testDB: TestDB) = runSuspendIO {
            withDbSuspending(testDB, Dispatchers.IO) {
                currentDialectTest.shouldNotBeNull()
            }
        }

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedDb should set currentTestDB`(testDB: TestDB) = runSuspendIO {
            withDbSuspending(testDB) {
                currentTestDB.shouldNotBeNull()
                currentTestDB shouldBeEqualTo testDB
            }
        }
    }
}
