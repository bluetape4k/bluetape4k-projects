package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TiDBServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Test
    fun `launch TiDB Server`() {
        TiDBServer().use { tidb ->
            tidb.start()
            assertConnection(tidb)

            performQuery(tidb, "SELECT 1").use { rs ->
                rs.getInt(1) shouldBeEqualTo 1
            }

            assertCorrectPorts(tidb)
        }
    }

    @Test
    fun `launch TiDB Server with default port`() {
        TiDBServer(useDefaultPort = true).use { tidb ->
            tidb.start()
            tidb.port shouldBeEqualTo TiDBServer.TIDB_PORT

            assertConnection(tidb)
            performQuery(tidb, "SELECT 1").use { rs ->
                rs.getInt(1) shouldBeEqualTo 1
            }
            assertCorrectPorts(tidb)
        }
    }

    private fun assertCorrectPorts(tidb: TiDBServer) {
        tidb.exposedPorts shouldContainSame listOf(TiDBServer.TIDB_PORT, TiDBServer.REST_API_PORT)
        tidb.livenessCheckPortNumbers shouldContainSame setOf(tidb.tidbPort, tidb.restApiPort)
    }

    @Test
    fun `with init script`() {
        TiDBServer().use { tidb ->
            tidb.withInitScript("tidb/init_tidb.sql")
            tidb.start()
            assertConnection(tidb)

            performQuery(tidb, "SELECT foo FROM bar").use { rs ->
                rs.getString(1) shouldBeEqualTo "hello world"
            }
        }
    }


    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { TiDBServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { TiDBServer(tag = " ") }
    }
}
