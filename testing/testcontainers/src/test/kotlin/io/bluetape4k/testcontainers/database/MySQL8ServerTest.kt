package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MySQL8ServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Test
    fun `launch mysql 8 server`() {
        MySQL8Server().use { mysql ->
            mysql.start()
            assertConnection(mysql)
        }
    }

    @Test
    fun `launch mysql 8 server with default port`() {
        MySQL8Server(useDefaultPort = true).use { mysql ->
            mysql.start()
            mysql.port shouldBeEqualTo MySQL5Server.PORT
            assertConnection(mysql)
        }
    }
}
