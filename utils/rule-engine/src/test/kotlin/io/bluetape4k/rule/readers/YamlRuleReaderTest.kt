package io.bluetape4k.rule.readers

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class YamlRuleReaderTest {

    companion object : KLogging()

    private val reader = YamlRuleReader()

    @Test
    fun `단일 YAML에서 RuleDefinition 읽기`() {
        val yaml = """
            name: discountRule
            description: 할인 규칙
            priority: 1
            condition: "amount > 1000"
            actions:
              - "discount = true"
        """.trimIndent()

        val ruleDef = reader.read(yaml.reader())

        ruleDef.shouldNotBeNull()
        ruleDef.name shouldBeEqualTo "discountRule"
        ruleDef.description shouldBeEqualTo "할인 규칙"
        ruleDef.priority shouldBeEqualTo 1
        ruleDef.condition shouldBeEqualTo "amount > 1000"
        ruleDef.actions shouldHaveSize 1
        ruleDef.actions[0] shouldBeEqualTo "discount = true"
    }

    @Test
    fun `rules 배열 YAML에서 여러 RuleDefinition 읽기`() {
        val source = javaClass.classLoader.getResourceAsStream("rules.yml")!!.reader()
        val defs = reader.readAll(source).toList()

        defs shouldHaveSize 2
        defs[0].name shouldBeEqualTo "discount"
        defs[0].priority shouldBeEqualTo 1
        defs[1].name shouldBeEqualTo "freeShipping"
        defs[1].priority shouldBeEqualTo 2
    }

    @Test
    fun `single-rule yml 파일에서 단일 Rule 읽기`() {
        val source = javaClass.classLoader.getResourceAsStream("single-rule.yml")!!.reader()
        val ruleDef = reader.read(source)

        ruleDef.shouldNotBeNull()
        ruleDef.name shouldBeEqualTo "discount"
    }

    @Test
    fun `여러 actions가 있는 YAML 읽기`() {
        val yaml = """
            name: multiAction
            condition: "value > 0"
            actions:
              - "a = true"
              - "b = false"
              - "c = 42"
        """.trimIndent()

        val ruleDef = reader.read(yaml.reader())
        ruleDef.actions shouldHaveSize 3
        ruleDef.actions[1] shouldBeEqualTo "b = false"
    }
}
