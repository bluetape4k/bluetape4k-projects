package io.bluetape4k.spring.batch.exposed

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

/**
 * Exposed Spring Batch 테스트 베이스 클래스.
 *
 * [AbstractExposedTest]를 상속하여 [TestDB.enabledDialects]로 지정된
 * 데이터베이스(H2, PostgreSQL, MySQL_V8)에 대해 파라미터화 테스트를 수행합니다.
 *
 * 사용 예시:
 * ```kotlin
 * class MyTest : AbstractExposedBatchTest() {
 *     @ParameterizedTest
 *     @MethodSource(ENABLE_DIALECTS_METHOD)
 *     fun `test name`(testDB: TestDB) = withBatchTables(testDB) {
 *         // 테스트 로직
 *     }
 * }
 * ```
 */
abstract class AbstractExposedBatchTest : AbstractExposedTest() {

    companion object : KLogging() {
        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()
    }

    /**
     * [SourceTable], [TargetTable]을 생성하고 테스트 블록을 실행합니다.
     *
     * @param testDB 대상 데이터베이스
     * @param statement 테스트 로직 블록
     */
    fun withBatchTables(testDB: TestDB, statement: JdbcTransaction.(TestDB) -> Unit) {
        withTables(testDB, SourceTable, TargetTable, statement = statement)
    }
}
