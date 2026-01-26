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

        val tester = object: Table("tester") {
            val id = varchar("id", 10).uniqueIndex()
        }

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
}
