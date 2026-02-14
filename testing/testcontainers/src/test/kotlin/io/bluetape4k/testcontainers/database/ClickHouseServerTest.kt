package io.bluetape4k.testcontainers.database

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ClickHouseServerTest: AbstractJdbcServerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch ClickHouse Server`() {
            ClickHouseServer().use { clickhouse ->
                clickhouse.start()
                assertConnection(clickhouse)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch ClickHouse Server with default port`() {
            ClickHouseServer(useDefaultPort = true).use { clickhouse ->
                clickhouse.start()
                clickhouse.port shouldBeEqualTo ClickHouseServer.HTTP_PORT
                assertConnection(clickhouse)
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ClickHouseServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ClickHouseServer(tag = " ") }
    }
}
