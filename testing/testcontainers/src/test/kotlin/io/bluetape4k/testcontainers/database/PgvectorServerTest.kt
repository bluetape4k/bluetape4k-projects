package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PgvectorServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `pgvector 서버 시작 및 연결 확인`() {
            PgvectorServer().use { server ->
                server.start()
                assertConnection(server)
            }
        }

        @Test
        fun `vector 확장이 자동으로 활성화된다`() {
            PgvectorServer().use { server ->
                server.start()
                val rs = performQuery(server, "SELECT extname FROM pg_extension WHERE extname = 'vector'")
                rs.getString("extname") shouldBeEqualTo "vector"
            }
        }

        @Test
        fun `VECTOR 타입 컬럼을 생성하고 데이터를 삽입할 수 있다`() {
            PgvectorServer().use { server ->
                server.start()
                server.getDataSource().use { ds ->
                    ds.connection.use { conn ->
                        conn.createStatement().execute(
                            "CREATE TABLE IF NOT EXISTS test_vec (id SERIAL PRIMARY KEY, embedding VECTOR(3))"
                        )
                        conn.createStatement().execute(
                            "INSERT INTO test_vec (embedding) VALUES ('[1,2,3]')"
                        )
                        val rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM test_vec")
                        rs.next()
                        rs.getInt(1) shouldBeEqualTo 1
                    }
                }
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `기본 포트로 pgvector 서버 시작`() {
            PgvectorServer(useDefaultPort = true).use { server ->
                server.start()
                server.port shouldBeEqualTo PgvectorServer.PORT
                assertConnection(server)
            }
        }
    }

    @Nested
    inner class WithExtensions {
        @Test
        fun `withExtensions 으로 추가 확장을 활성화한다`() {
            PgvectorServer().withExtensions("pg_trgm").use { server ->
                server.start()
                val rs = performQuery(
                    server,
                    "SELECT extname FROM pg_extension WHERE extname = 'pg_trgm'"
                )
                rs.getString("extname") shouldBeEqualTo "pg_trgm"
            }
        }

        @Test
        fun `기본 vector 확장은 withExtensions 호출 없이도 활성화된다`() {
            PgvectorServer().use { server ->
                server.start()
                val rs = performQuery(server, "SELECT extname FROM pg_extension WHERE extname = 'vector'")
                rs.getString("extname") shouldBeEqualTo "vector"
            }
        }

        @Test
        fun `vector 확장을 withExtensions 에 중복 지정해도 오류가 없다`() {
            PgvectorServer().withExtensions("vector").use { server ->
                server.start()
                // distinct() 덕분에 중복 지정해도 확장이 정확히 1건만 존재
                val rs = performQuery(server, "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'")
                rs.getInt(1) shouldBeEqualTo 1
            }
        }
    }

    @Test
    fun `blank image 또는 tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { PgvectorServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { PgvectorServer(tag = " ") }
    }
}
