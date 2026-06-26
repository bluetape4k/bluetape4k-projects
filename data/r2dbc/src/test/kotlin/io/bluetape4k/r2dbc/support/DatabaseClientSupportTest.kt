package io.bluetape4k.r2dbc.support

import io.bluetape4k.assertions.assertFailsWith
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseClientSupportTest {

    @Test
    fun `bindIndexedMap rejects negative indices`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)

        assertFailsWith<IllegalArgumentException> {
            spec.bindIndexedMap(mapOf(-1 to "john"))
        }
    }

    @Test
    fun `bindNullable rejects negative indexed binding`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)

        assertFailsWith<IllegalArgumentException> {
            spec.bindNullable<String>(-1, "john")
        }
    }
}
