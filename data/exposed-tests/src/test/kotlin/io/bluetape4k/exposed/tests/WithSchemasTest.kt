package io.bluetape4k.exposed.tests

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class WithSchemasTest {

    companion object: KLogging()

    @Nested
    inner class Jdbc: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSchemas should create and drop schema when supported`(testDB: TestDB) {
            Assumptions.assumeTrue { testDB in TestDB.ALL_H2 || testDB in TestDB.ALL_POSTGRES }

            val schema = prepareSchemaForTest("test_schema_${System.currentTimeMillis()}")

            withSchemas(testDB, schema) {
                currentDialectTest.supportsCreateSchema.shouldBeTrue()
            }
        }
    }

    @Nested
    inner class Coroutines: AbstractExposedTest() {

        @ParameterizedTest
        @MethodSource(ENABLE_DIALECTS_METHOD)
        fun `withSuspendedSchemas should create and drop schema when supported`(testDB: TestDB) = runSuspendIO {
            Assumptions.assumeTrue { testDB in TestDB.ALL_H2 || testDB in TestDB.ALL_POSTGRES }

            val schema = prepareSchemaForTest("test_suspended_schema_${System.currentTimeMillis()}")

            withSchemasSuspending(testDB, schema) {
                currentDialectTest.supportsCreateSchema.shouldBeTrue()
            }
        }
    }
}
