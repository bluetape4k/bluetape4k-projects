package io.bluetape4k.exposed.r2dbc.statements

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BatchInsertOnConflictDoNothingTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel()

    /** 테스트용 단일 컬럼 테이블 (uniqueIndex로 중복 방지) */
    private val tester = object: Table("tester") {
        val id = varchar("id", 10).uniqueIndex()
    }

    private val codeTester = object: Table("code_tester") {
        val code = varchar("code", 20).uniqueIndex()
    }

    /**
     * Batch Insert with ON CONFLICT DO NOTHING - [batchInsert] 시, 예외가 발생하면 해당 예외를 무시하고, 다음 작업을 수행합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (id VARCHAR(10) NOT NULL);
     * ALTER TABLE tester ADD CONSTRAINT tester_id_unique UNIQUE (id);
     *
     * INSERT INTO tester (id) VALUES ('foo');
     * INSERT INTO tester (id) VALUES ('foo') ON CONFLICT (id) DO NOTHING;
     * INSERT INTO tester (id) VALUES ('bar') ON CONFLICT (id) DO NOTHING;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert number of inserted rows`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in (TestDB.ALL_MYSQL + TestDB.ALL_POSTGRES_LIKE) }

        withTables(testDB, tester) {
            tester.insert { it[id] = "foo" }

            val statement = BatchInsertOnConflictDoNothing(tester)
            val executable = BatchInsertOnConflictDoNothingExecutable(statement)

            val numInserted = executable.run {
                statement.addBatch()
                statement[tester.id] = "foo"        // 중복되므로 insert 되지 않음

                statement.addBatch()
                statement[tester.id] = "bar"        // 중복되지 않으므로 추가됨

                execute(this@withTables)
            }
            numInserted shouldBeEqualTo 1
        }
    }

    /**
     * 배치의 모든 레코드가 중복인 경우 삽입 건수가 0이어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert all duplicates returns 0`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in (TestDB.ALL_MYSQL + TestDB.ALL_POSTGRES_LIKE) }

        withTables(testDB, tester) {
            tester.insert { it[id] = "foo" }
            tester.insert { it[id] = "bar" }

            val statement = BatchInsertOnConflictDoNothing(tester)
            val executable = BatchInsertOnConflictDoNothingExecutable(statement)

            val numInserted = executable.run {
                statement.addBatch()
                statement[tester.id] = "foo"        // 중복

                statement.addBatch()
                statement[tester.id] = "bar"        // 중복

                execute(this@withTables)
            }
            numInserted shouldBeEqualTo 0
        }
    }

    /**
     * 배치의 모든 레코드가 신규인 경우 전부 삽입되어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert all new rows returns full count`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in (TestDB.ALL_MYSQL + TestDB.ALL_POSTGRES_LIKE) }

        withTables(testDB, tester) {
            val statement = BatchInsertOnConflictDoNothing(tester)
            val executable = BatchInsertOnConflictDoNothingExecutable(statement)

            val numInserted = executable.run {
                statement.addBatch()
                statement[tester.id] = "alpha"

                statement.addBatch()
                statement[tester.id] = "beta"

                statement.addBatch()
                statement[tester.id] = "gamma"

                execute(this@withTables)
            }
            numInserted shouldBeEqualTo 3
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `postgres like dialect 에서는 id 가 아닌 unique 컬럼도 대상으로 사용할 수 있다`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE }

        withTables(testDB, codeTester) {
            codeTester.insert { it[code] = "alpha" }

            val statement = BatchInsertOnConflictDoNothing(codeTester)
            val executable = BatchInsertOnConflictDoNothingExecutable(statement)

            val numInserted = executable.run {
                statement.addBatch()
                statement[codeTester.code] = "alpha"

                statement.addBatch()
                statement[codeTester.code] = "beta"

                execute(this@withTables)
            }

            numInserted shouldBeEqualTo 1
        }
    }
}
