package io.bluetape4k.r2dbc.support

import ch.qos.logback.classic.Level
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
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

    @Test
    fun `bind map logs omit values while preserving binding metadata`() {
        val loggerName = "io.bluetape4k.r2dbc.support.DatabaseClientSupport"
        val logger = LoggerFactory.getLogger(loggerName) as ch.qos.logback.classic.Logger
        val previousLevel = logger.level
        val spec = mockk<DatabaseClient.GenericExecuteSpec>()
        val namedSecret = "named-binding-secret"
        val indexedSecret = "indexed-binding-secret"

        every { spec.bind("password", any()) } returns spec
        every { spec.bind(0, any()) } returns spec

        InMemoryLogbackAppender(loggerName).use { appender ->
            try {
                logger.level = Level.TRACE
                spec.bindMap(mapOf("password" to namedSecret))
                spec.bindIndexedMap(mapOf(0 to indexedSecret))

                val messages = appender.messages.joinToString("\n")
                messages shouldContain "name=password"
                messages shouldContain "index=0"
                messages shouldNotContain namedSecret
                messages shouldNotContain indexedSecret
            } finally {
                logger.level = previousLevel
            }
        }
        verify(exactly = 1) { spec.bind("password", any()) }
        verify(exactly = 1) { spec.bind(0, any()) }
    }
}
