package io.bluetape4k.exposed.r2dbc.tests

import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Schema
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class WithSchemasTest: AbstractExposedR2dbcTest() {

    companion object {
        @JvmStatic
        fun schemaDialects(): Set<TestDB> = TestDB.ALL_H2 + TestDB.ALL_POSTGRES

        const val SCHEMA_DIALECTS_METHOD = "schemaDialects"
    }

    @ParameterizedTest
    @MethodSource(SCHEMA_DIALECTS_METHOD)
    fun `withSchemas 는 dialect 지원 여부에 따라 statement 실행을 제어한다`(testDB: TestDB) = runTest {
        var executed = false

        withSchemas(testDB, Schema("schema_${System.nanoTime()}")) {
            executed = true
        }

        withDb(testDB) {
            executed shouldBeEqualTo currentDialectTest.supportsCreateSchema
        }
    }

    @ParameterizedTest
    @MethodSource(SCHEMA_DIALECTS_METHOD)
    fun `withSchemas 는 지원 방언에서 statement 예외를 전파한다`(testDB: TestDB) = runTest {
        var thrown = false

        val failure = runCatching {
            withSchemas(testDB, Schema("schema_${System.nanoTime()}")) {
                throw IllegalStateException("boom")
            }
        }.exceptionOrNull()

        if (failure is IllegalStateException) {
            thrown = true
        } else if (failure != null) {
            throw failure
        }

        withDb(testDB) {
            thrown shouldBeEqualTo currentDialectTest.supportsCreateSchema
        }
    }
}
