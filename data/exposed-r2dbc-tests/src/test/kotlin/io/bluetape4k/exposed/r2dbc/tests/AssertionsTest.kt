package io.bluetape4k.exposed.r2dbc.tests

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class AssertionsTest: AbstractExposedR2dbcTest() {

    object AssertionTable: IntIdTable("assertion_r2dbc_table") {
        val name = varchar("name", 64)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `expectException 은 지정한 예외를 검증한다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            expectException<IllegalArgumentException> {
                throw IllegalArgumentException("boom")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `expectExceptionSuspending 은 지정한 예외를 검증한다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            expectExceptionSuspending<IllegalStateException> {
                throw IllegalStateException("boom")
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `expectException 은 다른 예외 타입이면 AssertionError 를 던진다`(testDB: TestDB) = runTest {
        withDb(testDB) {
            assertFailsWith<AssertionError> {
                expectException<IllegalArgumentException> {
                    throw IllegalStateException("unexpected")
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `assertFailAndRollback 은 실패 블록을 처리한 뒤 트랜잭션을 계속 사용할 수 있다`(testDB: TestDB) = runTest {
        withTables(testDB, AssertionTable) {
            assertFailAndRollback("block must fail") {
                error("forced failure")
            }

            AssertionTable.insert {
                it[name] = "persisted"
            }
            AssertionTable.selectAll().count() shouldBeEqualTo 1L
        }
    }
}
