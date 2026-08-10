package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.SettableById
import com.datastax.oss.driver.api.core.data.SettableByIndex
import com.datastax.oss.driver.api.core.data.SettableByName
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettableSupportTest {

    interface TestSettableById: SettableById<TestSettableById>
    interface TestSettableByIndex: SettableByIndex<TestSettableByIndex>
    interface TestSettableByName: SettableByName<TestSettableByName>

    private val settableById = mockk<TestSettableById>(relaxed = true)
    private val settableByIndex = mockk<TestSettableByIndex>(relaxed = true)
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

    @Test
    fun `SettableByIndex setValue 는 reified 타입을 전달한다`() {
        every { settableByIndex.set(0, "value", String::class.java) } returns settableByIndex

        settableByIndex.setValue(0, "value")

        verify(exactly = 1) { settableByIndex.set(0, "value", String::class.java) }
    }

    @Test
    fun `collection setters forward reified element and map types`() {
        val list = listOf("a", "b")
        val set = setOf("admin")

        every { settableById.setList(id, list, String::class.java) } returns settableById
        every { settableById.setSet(id, set, String::class.java) } returns settableById
        settableById.setList(id, list)
        settableById.setSet(id, set)

        every { settableByIndex.setList(0, list, String::class.java) } returns settableByIndex
        every { settableByIndex.setSet(1, set, String::class.java) } returns settableByIndex
        settableByIndex.setList(0, list)
        settableByIndex.setSet(1, set)

        every { settableByName.setList("tags", list, String::class.java) } returns settableByName
        every { settableByName.setSet("roles", set, String::class.java) } returns settableByName
        settableByName.setList("tags", list)
        settableByName.setSet("roles", set)

        verify(exactly = 1) { settableById.setList(id, list, String::class.java) }
        verify(exactly = 1) { settableById.setSet(id, set, String::class.java) }
        verify(exactly = 1) { settableByIndex.setList(0, list, String::class.java) }
        verify(exactly = 1) { settableByIndex.setSet(1, set, String::class.java) }
        verify(exactly = 1) { settableByName.setList("tags", list, String::class.java) }
        verify(exactly = 1) { settableByName.setSet("roles", set, String::class.java) }
    }
}
