package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.DriverManager

/**
 * [PostgreSQLAgeServer] 테스트
 *
 * Apache AGE 확장이 설치된 PostgreSQL 컨테이너가 정상적으로 시작되고,
 * 그래프 데이터베이스 기능이 동작하는지 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgreSQLAgeServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private val server: PostgreSQLAgeServer = PostgreSQLAgeServer.Launcher.postgresqlAge

    @Test
    fun `AGE 서버가 정상적으로 실행된다`() {
        server.isRunning.shouldBeTrue()
        server.port shouldBeGreaterThan 0

        val hostProp = System.getProperty("testcontainers.postgresql-age.host")
        hostProp.shouldNotBeNull()
    }

    @Test
    fun `JDBC 연결이 성공한다`() {
        DriverManager.getConnection(server.jdbcUrl, server.username, server.password).use { conn ->
            conn.isValid(3).shouldBeTrue()
        }
    }

    @Test
    fun `ag_catalog 의 ag_graph 테이블을 조회할 수 있다`() {
        DriverManager.getConnection(server.jdbcUrl, server.username, server.password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("LOAD 'age'")
                stmt.execute("""SET search_path = ag_catalog, "${'$'}user", public""")

                val rs = stmt.executeQuery("SELECT * FROM ag_catalog.ag_graph")
                rs.shouldNotBeNull()
            }
        }
    }

    @Test
    fun `그래프를 생성하고 확인할 수 있다`() {
        DriverManager.getConnection(server.jdbcUrl, server.username, server.password).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("LOAD 'age'")
                stmt.execute("""SET search_path = ag_catalog, "${'$'}user", public""")

                // 그래프 생성
                stmt.execute("SELECT ag_catalog.create_graph('test_graph')")

                // 생성된 그래프 확인
                val rs = stmt.executeQuery("SELECT * FROM ag_catalog.ag_graph WHERE name = 'test_graph'")
                rs.next().shouldBeTrue()

                val graphName = rs.getString("name")
                graphName shouldBeEqualTo "test_graph"
            }
        }
    }
}
