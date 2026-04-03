package io.bluetape4k.exposed.trino

import io.bluetape4k.exposed.trino.domain.Events
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.TrinoServer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.sql.SQLException

/**
 * Trino JDBC를 사용하는 테스트 기반 클래스.
 *
 * [TrinoServer.Launcher] 싱글턴을 통해 컨테이너를 재사용하고,
 * memory 카탈로그의 default 스키마에 연결합니다.
 *
 * ## 주의사항
 *
 * Trino memory 커넥터는 DELETE를 지원하지 않으므로,
 * [withEventsTable]은 테이블을 drop 후 재생성하는 방식으로 데이터를 초기화합니다.
 *
 * ## 사용 예
 *
 * ```kotlin
 * class MyTest : AbstractTrinoTest() {
 *
 *     @Test
 *     fun `이벤트 조회`() = withEventsTable {
 *         transaction(db) {
 *             Events.insert {
 *                 it[eventId] = 1L
 *                 it[eventName] = "click"
 *                 it[region] = "kr"
 *             }
 *             Events.selectAll().count() shouldBeEqualTo 1L
 *         }
 *     }
 * }
 * ```
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractTrinoTest {

    companion object : KLogging() {

        val trino: TrinoServer by lazy { TrinoServer.Launcher.trino }

        val db: Database by lazy {
            TrinoDatabase.connect(
                host = trino.host,
                port = trino.port,
                catalog = "memory",
                schema = "default",
                user = trino.username ?: "trino",
            )
        }

        /**
         * Trino 쿼리 엔진이 준비될 때까지 대기합니다.
         *
         * 컨테이너 재사용(shouldBeReused=true) 시 포트가 열려도 워커 노드 등록 전에
         * "No nodes available" 오류가 일시적으로 발생할 수 있습니다.
         */
        @JvmStatic
        @BeforeAll
        fun waitForTrinoReady() {
            repeat(15) { attempt ->
                runCatching {
                    transaction(db) { exec("SELECT 1") { rs -> rs.next(); rs.getInt(1) } }
                }.onSuccess {
                    return
                }.onFailure { e ->
                    if (attempt < 14 && e.isNoNodesAvailable()) {
                        log.warn("Trino not ready (attempt ${attempt + 1}/15), waiting 1s...")
                        Thread.sleep(1000L)
                    } else if (!e.isNoNodesAvailable()) {
                        throw e
                    }
                }
            }
        }

        internal fun Throwable.isNoNodesAvailable(): Boolean =
            (this is SQLException && message?.contains("No nodes available") == true) ||
                cause?.isNoNodesAvailable() == true
    }

    /**
     * 각 테스트 전에 Trino가 쿼리를 받을 수 있는지 확인합니다.
     * DROP/CREATE 사이 "No nodes available" 일시 오류를 방지합니다.
     */
    @BeforeEach
    fun ensureTrinoReady() {
        // DROP 후 memory connector 노드가 안정화될 때까지 잠시 대기
        Thread.sleep(300L)
        waitForTrinoReady()
    }

    private fun resetEventsTable() {
        transaction(db) {
            runCatching { SchemaUtils.drop(Events) }
            SchemaUtils.create(Events)
        }
    }

    private fun dropEventsTable() {
        transaction(db) {
            runCatching { SchemaUtils.drop(Events) }
        }
    }

    /**
     * events 테이블을 생성하고 테스트 블록을 실행합니다.
     *
     * Trino memory 커넥터는 DELETE를 지원하지 않으므로,
     * 테이블을 drop 후 재생성하여 데이터를 초기화합니다.
     *
     * "No nodes available" 발생 시 최대 5회까지 전체 작업을 재시도합니다.
     */
    protected fun withEventsTable(block: () -> Unit) {
        repeat(5) { attempt ->
            runCatching {
                resetEventsTable()
                try {
                    block()
                    return
                } finally {
                    runCatching { dropEventsTable() }
                }
            }.onFailure { e ->
                if (attempt < 4 && e.isNoNodesAvailable()) {
                    log.warn("withEventsTable 재시도 ${attempt + 1}/5: ${e.message}")
                    Thread.sleep(1000L)
                } else {
                    throw e
                }
            }
        }
    }

    protected fun withEventsTableSuspend(block: suspend () -> Unit) {
        repeat(5) { attempt ->
            runCatching {
                resetEventsTable()
                try {
                    runBlocking { block() }
                    return
                } finally {
                    runCatching { dropEventsTable() }
                }
            }.onFailure { e ->
                if (attempt < 4 && e.isNoNodesAvailable()) {
                    log.warn("withEventsTableSuspend 재시도 ${attempt + 1}/5: ${e.message}")
                    Thread.sleep(1000L)
                } else {
                    throw e
                }
            }
        }
    }
}
