package io.bluetape4k.exposed.r2dbc.tests

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.exists
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class TestSupportsTest: AbstractExposedR2dbcTest() {

    object UtilityTable: IntIdTable("utility_r2dbc_table") {
        val name = varchar("name", 64)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withAutoCommit 은 예외가 발생해도 autoCommit 을 원복한다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            val conn = connection()
            val originalAutoCommit = conn.getAutoCommit()

            assertFailsWith<IllegalStateException> {
                withAutoCommit(autoCommit = !originalAutoCommit) {
                    throw IllegalStateException("boom")
                }
            }

            conn.getAutoCommit() shouldBeEqualTo originalAutoCommit
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withTables 는 statement 예외를 호출자에게 전파하고 테이블을 정리한다`(testDB: TestDB) = runTest {
        assertFailsWith<IllegalStateException> {
            withTables(testDB, UtilityTable) {
                throw IllegalStateException("boom")
            }
        }

        withDb(testDB) {
            UtilityTable.exists().shouldBeFalse()
        }
    }
}
