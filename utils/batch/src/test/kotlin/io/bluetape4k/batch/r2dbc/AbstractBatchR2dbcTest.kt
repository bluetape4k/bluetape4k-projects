package io.bluetape4k.batch.r2dbc

import io.bluetape4k.batch.BatchSourceTable
import io.bluetape4k.batch.BatchTargetTable
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

/**
 * R2DBC 기반 배치 통합 테스트 베이스 클래스.
 *
 * [AbstractExposedR2dbcTest]를 상속하여 H2/PostgreSQL/MySQL 다중 DB 파라미터 테스트를 지원한다.
 */
abstract class AbstractBatchR2dbcTest : AbstractExposedR2dbcTest() {

    /** [BatchSourceTable], [BatchTargetTable]을 생성하고 suspend 테스트 블록을 실행한다. */
    suspend fun withBatchTables(testDB: TestDB, statement: suspend R2dbcTransaction.(TestDB) -> Unit) {
        withTables(testDB, BatchSourceTable, BatchTargetTable, statement = statement)
    }
}
