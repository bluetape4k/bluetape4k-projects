package io.bluetape4k.exposed.sql

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BatchInsertOnConflictIgnoreTest: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert number of inserted rows`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB + TestDB.ALL_POSTGRES_LIKE }

        val tester = object: Table("tester") {
            val id = varchar("id", 10).uniqueIndex()
        }
        withTables(testDB, tester) {
            tester.insert { it[id] = "foo" }

            // 중복된 id 를 가진 row 를 추가하면, 무시합니다.
            val numInserted = BatchInsertOnConflictIgnore(tester)
                .run {
                    addBatch()
                    this[tester.id] = "foo"        // 중복되므로 insert 되지 않음

                    addBatch()
                    this[tester.id] = "bar"        // 중복되지 않으므로 추가됨

                    execute(this@withTables)
                }
            numInserted shouldBeEqualTo 1
        }
    }
}
