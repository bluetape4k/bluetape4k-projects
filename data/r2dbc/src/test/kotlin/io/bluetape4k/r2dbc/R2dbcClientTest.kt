package io.bluetape4k.r2dbc

import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Test
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient

class R2dbcClientTest {

    @Test
    fun `connectionFactory는 DatabaseClient의 factory를 그대로 노출한다`() {
        val factory = mockk<ConnectionFactory>()
        val databaseClient = mockk<DatabaseClient>()
        val entityTemplate = mockk<R2dbcEntityTemplate>()
        val converter = mockk<MappingR2dbcConverter>()
        every { databaseClient.connectionFactory } returns factory

        val client = R2dbcClient(databaseClient, entityTemplate, converter)

        client.connectionFactory shouldBeSameInstanceAs factory
    }
}
