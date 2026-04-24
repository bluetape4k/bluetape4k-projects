package io.bluetape4k.rule.readers

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class HoconRuleReaderTest {

    companion object : KLogging()

    private val reader = HoconRuleReader()

    @Test
    fun `단일 HOCON에서 RuleDefinition 읽기`() {
        val hocon = """
            name = discountRule
            description = "할인 규칙"
            priority = 1
            condition = "amount > 1000"
            actions = ["discount = true"]
        """.trimIndent()

        val ruleDef = reader.read(hocon.reader())

        ruleDef.shouldNotBeNull()
        ruleDef.name shouldBeEqualTo "discountRule"
        ruleDef.description shouldBeEqualTo "할인 규칙"
        ruleDef.priority shouldBeEqualTo 1
        ruleDef.condition shouldBeEqualTo "amount > 1000"
        ruleDef.actions shouldHaveSize 1
        ruleDef.actions[0] shouldBeEqualTo "discount = true"
    }

    @Test
    fun `rules 배열 HOCON에서 여러 RuleDefinition 읽기`() {
        val source = javaClass.classLoader.getResourceAsStream("rules.conf")!!.reader()
        val defs = reader.readAll(source).toList()

        defs shouldHaveSize 2
        defs[0].name shouldBeEqualTo "discount"
        defs[0].condition shouldBeEqualTo "amount > 1000"
        defs[1].name shouldBeEqualTo "freeShipping"
        defs[1].condition shouldBeEqualTo "amount > 5000"
    }

    @Test
    fun `여러 actions가 있는 HOCON 읽기`() {
        val hocon = """
            name = multiAction
            condition = "value > 0"
            actions = ["a = true", "b = false", "c = 42"]
        """.trimIndent()

        val ruleDef = reader.read(hocon.reader())
        ruleDef.actions shouldHaveSize 3
        ruleDef.actions[2] shouldBeEqualTo "c = 42"
    }
}
