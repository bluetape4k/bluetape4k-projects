package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CockroachServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch Cockroach Server`() {
            CockroachServer().use { cockroach ->
                cockroach.start()
                assertConnection(cockroach)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch Cockroach Server with default port`() {
            CockroachServer(useDefaultPort = true).use { cockroach ->
                cockroach.start()
                cockroach.port shouldBeEqualTo CockroachServer.DB_PORT
                assertConnection(cockroach)
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { CockroachServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { CockroachServer(tag = " ") }
    }
}
