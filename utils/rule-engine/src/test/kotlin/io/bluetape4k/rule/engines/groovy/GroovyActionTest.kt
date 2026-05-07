package io.bluetape4k.rule.engines.groovy

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GroovyActionTest {

    companion object : KLogging()

    @Test
    fun `GroovyAction 실행 - facts에 Boolean 값 추가`() {
        val action = GroovyAction("discount = true")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
        facts.get<Boolean>("discount").shouldNotBeNull().shouldBeTrue()
    }

    @Test
    fun `GroovyAction 실행 - 계산 결과 저장`() {
        val action = GroovyAction("result = amount * 0.1")
        val facts = Facts.of("amount" to 2000)
        action.execute(facts)
        val result = facts.get<Number>("result")
        result.shouldNotBeNull()
        result.toDouble() shouldBeEqualTo 200.0
    }

    @Test
    fun `GroovyAction 실행 - 문자열 조작`() {
        val action = GroovyAction("upper = name.toUpperCase()")
        val facts = Facts.of("name" to "alice")
        action.execute(facts)
        facts.get<String>("upper").shouldNotBeNull() shouldBeEqualTo "ALICE"
    }

    @Test
    fun `GroovyAction 실행 - 조건부 값 설정`() {
        val action = GroovyAction("tier = amount > 5000 ? 'gold' : 'silver'")
        val facts = Facts.of("amount" to 3000)
        action.execute(facts)
        facts.get<String>("tier").shouldNotBeNull() shouldBeEqualTo "silver"
    }

    @Test
    fun `GroovyAction 실행 - 클로저 활용`() {
        val action = GroovyAction(
            """
            def items = amount > 2000 ? ['gold', 'silver'] : ['bronze']
            tier = items[0]
            """.trimIndent()
        )
        val facts = Facts.of("amount" to 3000)
        action.execute(facts)
        facts.get<String>("tier") shouldBeEqualTo "gold"
    }

    @Test
    fun `GroovyAction equals and hashCode`() {
        val script = "discount = true"
        val a1 = GroovyAction(script)
        val a2 = GroovyAction(script)
        (a1 == a2).shouldBeTrue()
        (a1.hashCode() == a2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `GroovyAction toString 표현`() {
        val script = "x = 1"
        val action = GroovyAction(script)
        action.toString() shouldBeEqualTo "GroovyAction(script='$script')"
    }

    @Test
    fun `잘못된 Groovy 스크립트는 RuleException 발생`() {
        val action = GroovyAction("def x = {{{{{ broken groovy")
        assertThrows<RuleException> {
            action.execute(Facts.empty())
        }
    }
}
