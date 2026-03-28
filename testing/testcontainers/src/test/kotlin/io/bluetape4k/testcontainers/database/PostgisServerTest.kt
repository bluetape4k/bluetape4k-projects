package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PostgisServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `PostGIS 서버 시작 및 연결 확인`() {
            PostgisServer().use { server ->
                server.start()
                assertConnection(server)
            }
        }

        @Test
        fun `PostGIS 확장이 자동으로 활성화된다`() {
            PostgisServer().use { server ->
                server.start()
                val rs = performQuery(server, "SELECT extname FROM pg_extension WHERE extname = 'postgis'")
                rs.getString("extname") shouldBeEqualTo "postgis"
            }
        }

        @Test
        fun `postgis_version() 함수로 PostGIS 버전을 조회할 수 있다`() {
            PostgisServer().use { server ->
                server.start()
                val rs = performQuery(server, "SELECT postgis_version()")
                rs.getString(1).isNullOrBlank().not().shouldBeTrue()
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `기본 포트로 PostGIS 서버 시작`() {
            PostgisServer(useDefaultPort = true).use { server ->
                server.start()
                server.port shouldBeEqualTo PostgisServer.PORT
                assertConnection(server)
            }
        }
    }

    @Nested
    inner class WithExtensions {
        @Test
        fun `withExtensions 으로 추가 확장을 활성화한다`() {
            PostgisServer().withExtensions("postgis_topology").use { server ->
                server.start()
                val rs = performQuery(
                    server,
                    "SELECT extname FROM pg_extension WHERE extname = 'postgis_topology'"
                )
                rs.getString("extname") shouldBeEqualTo "postgis_topology"
            }
        }

        @Test
        fun `기본 postgis 확장은 withExtensions 호출 없이도 활성화된다`() {
            PostgisServer().use { server ->
                server.start()
                val rs = performQuery(server, "SELECT extname FROM pg_extension WHERE extname = 'postgis'")
                rs.getString("extname") shouldBeEqualTo "postgis"
            }
        }
    }

    @Test
    fun `blank image 또는 tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { PostgisServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { PostgisServer(tag = " ") }
    }
}
