package io.bluetape4k.exposed.jdbc

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withDb
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeTrue
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * [execCreateMissingTablesAndColumns] 통합 테스트입니다.
 *
 * 누락 테이블 생성, 이미 존재하는 테이블에 대한 멱등성, 복수 테이블 처리를 검증합니다.
 */
class SchemaUtilsExtensionsTest : AbstractExposedTest() {

    companion object : KLogging()

    private object SimpleTable : IntIdTable("jdbc_schema_ext_simple") {
        val name = varchar("name", 255)
    }

    private object AnotherTable : IntIdTable("jdbc_schema_ext_another") {
        val value = integer("value")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `execCreateMissingTablesAndColumns 는 누락 테이블을 생성한다`(testDB: TestDB) {
        withDb(testDB) {
            try {
                runCatching { SchemaUtils.drop(SimpleTable) }

                execCreateMissingTablesAndColumns(SimpleTable)

                SimpleTable.exists().shouldBeTrue()
            } finally {
                runCatching { SchemaUtils.drop(SimpleTable) }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `execCreateMissingTablesAndColumns 는 이미 존재하는 테이블에 대해 예외 없이 동작한다`(testDB: TestDB) {
        withTables(testDB, SimpleTable) {
            // withTables 가 이미 생성한 테이블에 대해 두 번 호출해도 예외가 발생하지 않아야 한다
            execCreateMissingTablesAndColumns(SimpleTable)

            SimpleTable.exists().shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `execCreateMissingTablesAndColumns 는 복수 테이블을 한 번에 처리한다`(testDB: TestDB) {
        withDb(testDB) {
            try {
                runCatching { SchemaUtils.drop(AnotherTable, SimpleTable) }

                execCreateMissingTablesAndColumns(SimpleTable, AnotherTable)

                SimpleTable.exists().shouldBeTrue()
                AnotherTable.exists().shouldBeTrue()
            } finally {
                runCatching { SchemaUtils.drop(AnotherTable, SimpleTable) }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `execCreateMissingTablesAndColumns 는 일부만 누락된 경우 누락 테이블만 생성한다`(testDB: TestDB) {
        withDb(testDB) {
            try {
                runCatching { SchemaUtils.drop(AnotherTable, SimpleTable) }
                SchemaUtils.create(SimpleTable)

                // SimpleTable은 이미 존재, AnotherTable만 누락
                execCreateMissingTablesAndColumns(SimpleTable, AnotherTable)

                SimpleTable.exists().shouldBeTrue()
                AnotherTable.exists().shouldBeTrue()
            } finally {
                runCatching { SchemaUtils.drop(AnotherTable, SimpleTable) }
            }
        }
    }
}
