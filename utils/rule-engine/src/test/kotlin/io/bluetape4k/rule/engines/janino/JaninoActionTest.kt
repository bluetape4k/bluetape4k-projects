package io.bluetape4k.rule.engines.janino

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import org.junit.jupiter.api.Test

class JaninoActionTest {

    companion object: KLogging()

    @Test
    fun `JaninoAction 실행 - facts에 Boolean 값 추가`() {
        val action = JaninoAction("facts.put(\"discount\", Boolean.TRUE);")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
        facts.get<Boolean>("discount").shouldBeTrue()
    }

    @Test
    fun `JaninoAction 실행 - facts에 String 값 추가`() {
        val action = JaninoAction("facts.put(\"greeting\", \"hello\");")
        val facts = Facts.empty()
        action.execute(facts)
        facts.get<String>("greeting") shouldBeEqualTo "hello"
    }

    @Test
    fun `JaninoAction 실행 - 조건부 값 설정`() {
        val action = JaninoAction(
            "Integer amount = (Integer) facts.get(\"amount\"); " +
                    "facts.put(\"tier\", amount > 5000 ? \"gold\" : \"silver\");"
        )
        val facts = Facts.of("amount" to 3000)
        action.execute(facts)
        facts.get<String>("tier") shouldBeEqualTo "silver"
    }

    @Test
    fun `JaninoAction equals and hashCode`() {
        val script = "facts.put(\"x\", true);"
        val a1 = JaninoAction(script)
        val a2 = JaninoAction(script)
        a1 shouldBeEqualTo a2
    }

    @Test
    fun `JaninoAction toString 표현`() {
        val script = "facts.put(\"k\", true);"
        val action = JaninoAction(script)
        action.toString() shouldBeEqualTo "JaninoAction(script='$script')"
    }

    @Test
    fun `잘못된 Janino 스크립트는 RuleException 발생`() {
        val action = JaninoAction("@#invalid_java_code")
        assertFailsWith<RuleException> {
            action.execute(Facts.empty())
        }
    }
}
