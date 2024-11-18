package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CockroachServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Test
    fun `launch Cockroach Server`() {
        CockroachServer().use { cockroach ->
            cockroach.start()
            assertConnection(cockroach)
        }
    }

    @Test
    fun `launch Cockroach Server with default port`() {
        CockroachServer(useDefaultPort = true).use { cockroach ->
            cockroach.start()
            cockroach.port shouldBeEqualTo CockroachServer.DB_PORT
            assertConnection(cockroach)
        }
    }
}
