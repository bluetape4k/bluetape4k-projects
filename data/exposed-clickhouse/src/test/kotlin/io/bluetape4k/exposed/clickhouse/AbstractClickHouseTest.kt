package io.bluetape4k.exposed.clickhouse

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.ClickHouseServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * ClickHouse 컨테이너를 공유하는 통합 테스트의 기반 클래스.
 *
 * - [Execution]은 [ExecutionMode.SAME_THREAD]로 고정되어 있어, 동일 컨테이너에 대한
 *   동시 접근으로 인한 비결정성을 줄입니다.
 * - [BeforeAll]에서 단순 SELECT 쿼리를 통해 컨테이너 ready 상태를 polling 합니다.
 */
@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractClickHouseTest {

    companion object: KLogging() {
        val clickhouse: ClickHouseServer by lazy { ClickHouseServer.Launcher.clickhouse }

        val db: Database by lazy {
            ClickHouseDatabase.connect(
                host = clickhouse.host,
                port = clickhouse.port,
                database = "default",
                user = clickhouse.username ?: "test",
                password = clickhouse.password ?: "test",
            )
        }

        @JvmStatic
        @BeforeAll
        fun waitForClickHouseReady() {
            val maxAttempts = 10
            val backoffMillis = 500L

            repeat(maxAttempts) { attempt ->
                runCatching {
                    transaction(db) { exec("SELECT 1") { rs -> rs.next(); rs.getInt(1) } }
                }.onSuccess {
                    return
                }.onFailure { e ->
                    if (attempt < maxAttempts - 1) {
                        log.warn("ClickHouse not ready (attempt {}/{}), waiting {}ms...", attempt + 1, maxAttempts, backoffMillis)
                        Thread.sleep(backoffMillis)
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}
