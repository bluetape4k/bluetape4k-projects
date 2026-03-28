package io.bluetape4k.exposed.duckdb

import io.bluetape4k.exposed.duckdb.domain.Events
import io.bluetape4k.logging.KLogging
import org.duckdb.DuckDBConnection
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.DriverManager

/**
 * DuckDB JDBC를 사용하는 테스트 기반 클래스.
 *
 * 인메모리 DuckDB를 단일 루트 연결로 생성하고, [DuckDBConnection.duplicate]로
 * 같은 인메모리 DB를 공유하는 추가 연결을 제공합니다.
 *
 * ## 왜 duplicate 방식인가?
 * - `jdbc:duckdb:` URL은 연결마다 독립된 인메모리 DB를 생성합니다
 * - [DuckDBConnection.duplicate]는 같은 인메모리 DB에 새 연결 핸들을 반환합니다
 * - 여러 트랜잭션이 같은 테이블/데이터를 공유할 수 있습니다
 *
 * ## 사용 예
 *
 * ```kotlin
 * class MyTest : AbstractDuckDBTest() {
 *
 *     @BeforeEach
 *     fun setUp() {
 *         withEventsTable {}
 *     }
 *
 *     @Test
 *     fun `이벤트 조회`() {
 *         transaction(db) {
 *             Events.insert { it[eventId] = 1L; it[region] = "kr" }
 *             Events.selectAll().count() shouldBeEqualTo 1L
 *         }
 *     }
 * }
 * ```
 */
abstract class AbstractDuckDBTest {

    companion object : KLogging() {

        /**
         * 인메모리 DuckDB 루트 연결.
         * [DuckDBDatabase.DRIVER] 참조로 DuckDB 다이얼렉트를 등록합니다.
         */
        private val rootConn: DuckDBConnection by lazy {
            // DuckDBDatabase.DRIVER 접근 시 DuckDBDatabase.init{} 실행 → 다이얼렉트 등록
            Class.forName(DuckDBDatabase.DRIVER)
            DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection
        }

        /**
         * 테스트용 DuckDB 데이터베이스 연결.
         * [rootConn]의 duplicate 연결을 사용하여 동일한 인메모리 DB를 공유합니다.
         */
        val db: Database by lazy {
            Database.connect(getNewConnection = { DuckDBConnectionWrapper(rootConn.duplicate()) })
        }
    }

    /**
     * events 테이블을 초기화하고 테스트 블록을 실행합니다.
     * 테이블을 생성(없으면)하고 기존 데이터를 모두 삭제한 뒤 블록을 실행합니다.
     */
    protected fun withEventsTable(block: () -> Unit) {
        transaction(db) {
            SchemaUtils.create(Events)
            Events.deleteAll()
        }
        block()
    }
}
