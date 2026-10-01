package io.bluetape4k.rule.readers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class JsonRuleReaderTest {

    companion object: KLogging()

    private val reader = JsonRuleReader()

    @Test
    fun `단일 JSON에서 RuleDefinition 읽기`() {
        val json = """
            {
              "name": "discountRule",
              "description": "할인 규칙",
              "priority": 1,
              "condition": "amount > 1000",
              "actions": ["discount = true"]
            }
        """.trimIndent()

        val ruleDef = reader.read(json.reader())

        ruleDef.shouldNotBeNull()
        ruleDef.name shouldBeEqualTo "discountRule"
        ruleDef.description shouldBeEqualTo "할인 규칙"
        ruleDef.priority shouldBeEqualTo 1
        ruleDef.condition shouldBeEqualTo "amount > 1000"
        ruleDef.actions shouldHaveSize 1
        ruleDef.actions[0] shouldBeEqualTo "discount = true"
    }

    @Test
    fun `rules 배열 JSON에서 여러 RuleDefinition 읽기`() {
        val source = javaClass.classLoader.getResourceAsStream("rules.json")!!.reader()
        val defs = reader.readAll(source).toList()

        defs shouldHaveSize 2
        defs[0].name shouldBeEqualTo "discount"
        defs[0].condition shouldBeEqualTo "amount > 1000"
        defs[1].name shouldBeEqualTo "freeShipping"
        defs[1].condition shouldBeEqualTo "amount > 5000"
    }

    @Test
    fun `condition이 없는 JSON은 IllegalArgumentException 발생`() {
        val json = """
            {
              "name": "noCondition",
              "actions": ["result = true"]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            reader.read(json.reader())
        }
    }

    @Test
    fun `빈 rules 배열이면 빈 Sequence 반환`() {
        val json = """{"rules": []}"""
        val defs = reader.readAll(json.reader()).toList()

        defs.shouldBeEmpty()
    }

    @Test
    fun `여러 actions가 있는 JSON 읽기`() {
        val json = """
            {
              "name": "multiAction",
              "condition": "value > 0",
              "actions": ["a = true", "b = false", "c = 42"]
            }
        """.trimIndent()

        val ruleDef = reader.read(json.reader())
        ruleDef.actions shouldHaveSize 3
        ruleDef.actions[0] shouldBeEqualTo "a = true"
        ruleDef.actions[1] shouldBeEqualTo "b = false"
        ruleDef.actions[2] shouldBeEqualTo "c = 42"
    }

    @Test
    fun `잘못된 action이 있는 단일 JSON 규칙은 거부`() {
        val json = """
            {
              "name": "malformedAction",
              "condition": "value > 0",
              "actions": ["valid action", {}]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            reader.read(json.reader())
        }
    }

    @Test
    fun `잘못된 action이 있는 JSON 규칙 전체를 제외하고 나머지는 읽기`() {
        val json = """
            {
              "rules": [
                {
                  "name": "validBefore",
                  "condition": "value > 0",
                  "actions": ["valid action"]
                },
                {
                  "name": "malformedAction",
                  "condition": "value > 0",
                  "actions": ["valid action", {}]
                },
                {
                  "name": "validAfter",
                  "condition": "value > 0",
                  "actions": ["valid action"]
                }
              ]
            }
        """.trimIndent()

        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val previousLevel = rootLogger.level

        InMemoryLogbackAppender("root").use { appender ->
            try {
                rootLogger.level = Level.TRACE

                val definitions = reader.readAll(json.reader()).toList()

                definitions shouldHaveSize 2
                definitions.map { it.name } shouldBeEqualTo listOf("validBefore", "validAfter")
                appender.messages.joinToString("\n") shouldContain "index=1, fieldCount=3"
            } finally {
                rootLogger.level = previousLevel
            }
        }
    }
}
