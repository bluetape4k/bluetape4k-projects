package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MySQL5ServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Test
    fun `launch mysql 5 server`() {
        MySQL5Server().use { mysql ->
            mysql.start()
            assertConnection(mysql)
        }
    }

    @Test
    fun `launch mysql 5 server with default port`() {
        MySQL5Server(useDefaultPort = true).use { mysql ->
            mysql.start()
            mysql.port shouldBeEqualTo MySQL5Server.PORT
            assertConnection(mysql)
        }
    }
}
