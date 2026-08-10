package io.bluetape4k.r2dbc.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseClientSupportTest {

    @Test
    fun `bindMap preserves typed null parameters`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>()
        val typedNull = typedNullParameter<String>()

        every { spec.bind("description", typedNull) } returns spec

        val result = spec.bindMap(mapOf("description" to typedNull))

        result shouldBeSameInstanceAs spec
        verify(exactly = 1) { spec.bind("description", typedNull) }
        confirmVerified(spec)
    }

    @Test
    fun `bindMap rejects raw null values`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>()

        assertFailsWith<IllegalArgumentException> {
            spec.bindMap(mapOf("description" to null))
        }

        confirmVerified(spec)
    }

    @Test
    fun `bindIndexedMap preserves typed null parameters`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>()
        val typedNull = typedNullParameter<String>()

        every { spec.bind(0, typedNull) } returns spec

        val result = spec.bindIndexedMap(mapOf(0 to typedNull))

        result shouldBeSameInstanceAs spec
        verify(exactly = 1) { spec.bind(0, typedNull) }
        confirmVerified(spec)
    }

    @Test
    fun `bindIndexedMap rejects raw null values`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>()

        assertFailsWith<IllegalArgumentException> {
            spec.bindIndexedMap(mapOf(0 to null))
        }

        confirmVerified(spec)
    }

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

    @Test
    fun `bindNullable은 named와 indexed 값을 typed Parameter로 위임한다`() {
        val spec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)

        spec.bindNullable<String>("username", "john") shouldBeSameInstanceAs spec
        spec.bindNullable<String>(0, null) shouldBeSameInstanceAs spec
    }
}
