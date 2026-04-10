package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * JDBC 기반 배치 통합 테스트 베이스 클래스.
 *
 * [AbstractExposedTest]를 상속하여 H2/PostgreSQL/MySQL 다중 DB 파라미터 테스트를 지원한다.
 */
abstract class AbstractBatchJdbcTest : AbstractExposedTest() {

    /** [BatchSourceTable], [BatchTargetTable]을 생성하고 테스트 블록을 실행한다. */
    fun withBatchTables(testDB: TestDB, statement: JdbcTransaction.(TestDB) -> Unit) {
        withTables(testDB, BatchSourceTable, BatchTargetTable, statement = statement)
    }
}
