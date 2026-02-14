package io.bluetape4k.exposed.tests

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFails

class TestUtilityExtensionsTest: AbstractExposedTest() {

    private object UtilityTable: Table("utility_extension_test") {
        val id = integer("id")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `assertFalse 는 false 일 때 성공하고 true 일 때 실패한다`(testDB: TestDB) {
        withDb(testDB) {
            assertFalse(false)
            assertFails {
                assertFalse(true)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withAutoCommit 은 예외가 발생해도 autoCommit 을 원복한다`(testDB: TestDB) {
        withDb(testDB) {
            val original = connection.autoCommit

            assertFails {
                withAutoCommit(!original) {
                    connection.autoCommit shouldBeEqualTo !original
                    error("force rollback path")
                }
            }

            connection.autoCommit shouldBeEqualTo original
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withAutoCommitSuspending 은 예외가 발생해도 autoCommit 을 원복한다`(testDB: TestDB) = runBlocking {
        withDbSuspending(testDB) {
            val original = connection.autoCommit

            assertFails {
                withAutoCommitSuspending(!original) {
                    connection.autoCommit shouldBeEqualTo !original
                    error("force rollback path")
                }
            }

            connection.autoCommit shouldBeEqualTo original
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withTablesSuspending 에서 dropTables가 false 이면 테이블이 유지된다`(testDB: TestDB) = runBlocking {
        withTablesSuspending(testDB, UtilityTable, dropTables = false) {
            UtilityTable.exists().shouldBeTrue()
        }

        withDb(testDB) {
            UtilityTable.exists().shouldBeTrue()
            SchemaUtils.drop(UtilityTable)
        }
    }
}
