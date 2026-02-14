package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.SettableById
import com.datastax.oss.driver.api.core.data.SettableByName
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettableSupportTest {

    interface TestSettableById: SettableById<TestSettableById>
    interface TestSettableByName: SettableByName<TestSettableByName>

    private val settableById = mockk<TestSettableById>(relaxed = true)
    private val settableByName = mockk<TestSettableByName>(relaxed = true)
    private val id = CqlIdentifier.fromCql("col")

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `SettableById setValue 는 reified 타입을 전달한다`() {
        every { settableById.set(id, "value", String::class.java) } returns settableById

        settableById.setValue(id, "value")

        verify(exactly = 1) { settableById.set(id, "value", String::class.java) }
    }

    @Test
    fun `SettableByName setValue 는 reified 타입을 전달한다`() {
        every { settableByName.set("name", "alpha", String::class.java) } returns settableByName

        settableByName.setValue("name", "alpha")

        verify(exactly = 1) { settableByName.set("name", "alpha", String::class.java) }
    }
}
