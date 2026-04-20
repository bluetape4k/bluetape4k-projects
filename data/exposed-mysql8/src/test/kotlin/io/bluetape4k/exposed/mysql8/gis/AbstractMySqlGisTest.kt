package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.database.MySQL8Server
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

abstract class AbstractMySqlGisTest: AbstractExposedTest() {

    companion object: KLogging() {
        @JvmStatic
        val mysqlContainer: MySQL8Server = MySQL8Server.Launcher.mysql

        @JvmStatic
        val db: Database by lazy {
            Database.connect(
                url = mysqlContainer.jdbcUrl + "?allowPublicKeyRetrieval=true&useSSL=false",
                driver = "com.mysql.cj.jdbc.Driver",
                user = mysqlContainer.username ?: "test",
                password = mysqlContainer.password ?: "test",
            )
        }
    }

    /**
     * 테이블 생성 → 테스트 실행 → 테이블 삭제 패턴 (drop/create/finally drop).
     *
     * 테이블이 없어서 drop이 실패하는 경우는 무시하지만, 그 외 예외는 재전파한다.
     * [kotlinx.coroutines.CancellationException] 은 항상 재전파하여 코루틴 취소가 유실되지 않도록 한다.
     */
    protected fun withGeoTables(vararg tables: Table, statement: JdbcTransaction.() -> Unit) {
        transaction(db) {
            runCatching { SchemaUtils.drop(*tables) }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    log.debug(e) { "테이블 사전 삭제 실패 (무시됨): ${e.message}" }
                }
            SchemaUtils.create(*tables)
        }
        try {
            transaction(db) {
                statement()
            }
        } finally {
            transaction(db) {
                runCatching { SchemaUtils.drop(*tables) }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        log.warn(e) { "테이블 정리 실패: ${e.message}" }
                    }
            }
        }
    }
}
