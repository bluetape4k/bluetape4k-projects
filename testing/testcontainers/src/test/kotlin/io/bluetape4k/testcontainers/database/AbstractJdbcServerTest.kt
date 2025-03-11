package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeTrue
import java.sql.ResultSet

abstract class AbstractJdbcServerTest: AbstractContainerTest() {

    companion object: KLogging()

    protected fun assertConnection(jdbcServer: JdbcServer) {
        jdbcServer.getDataSource {
            this.connectionTimeout = 1000L
        }.use { ds ->
            ds.connection.use { conn ->
                conn.isValid(1).shouldBeTrue()
            }
        }
    }

    protected fun performQuery(jdbcServer: JdbcServer, sql: String): ResultSet {
        return jdbcServer.getDataSource().use { ds ->
            val stmt = ds.connection.createStatement()
            stmt.execute(sql)
            val resultSet = stmt.resultSet
            resultSet.next()
            resultSet
        }
    }
}
