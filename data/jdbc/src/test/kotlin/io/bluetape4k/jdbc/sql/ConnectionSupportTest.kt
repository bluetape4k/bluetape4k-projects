package io.bluetape4k.jdbc.sql

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class ConnectionSupportTest: AbstractJdbcSqlTest() {

    companion object: KLogging() {
        const val SELECT_ACTORS = "SELECT * FROM Actors"
    }

    @Test
    fun `withStatement with connection`() {
        dataSource.connection.use { conn ->
            conn.withStatement { stmt ->
                stmt.verifyQuery(SELECT_ACTORS)
            }
        }
    }
}
