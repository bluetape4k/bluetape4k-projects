package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.DriverManager
import io.bluetape4k.assertions.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrinoServerTest: AbstractContainerTest() {

    companion object: KLogging()


    private lateinit var trinoServer: TrinoServer

    @BeforeAll
    fun beforeAll() {
        trinoServer = TrinoServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if(this::trinoServer.isInitialized && trinoServer.isRunning) {
            trinoServer.close()
        }
    }

    @Test
    fun `Trino 서버가 정상적으로 실행된다`() {
        trinoServer.isRunning.shouldBeTrue()
        log.debug { "Trino URL: ${trinoServer.url}" }
    }

    @Test
    fun `SELECT 1 쿼리를 실행한다`() {
        val jdbcUrl = "jdbc:trino://${trinoServer.host}:${trinoServer.port}/memory"
        DriverManager.getConnection(jdbcUrl, "test", null).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT 1").use { rs ->
                    rs.next().shouldBeTrue()
                    rs.getInt(1) shouldBeEqualTo 1
                }
            }
        }
    }

    @Test
    fun `information_schema 테이블 목록을 조회한다`() {
        val jdbcUrl = "jdbc:trino://${trinoServer.host}:${trinoServer.port}/memory"
        DriverManager.getConnection(jdbcUrl, "test", null).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT table_name FROM information_schema.tables LIMIT 5").use { rs ->
                    var count = 0
                    while (rs.next()) {
                        count++
                        val tableName = rs.getString("table_name")
                        log.debug { "table_name=$tableName" }
                    }
                    log.debug { "조회된 테이블 수: $count" }
                }
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { TrinoServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { TrinoServer(tag = " ") }
    }
}
