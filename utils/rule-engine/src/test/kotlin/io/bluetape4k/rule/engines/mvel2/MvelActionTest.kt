package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MvelActionTest {

    companion object : KLogging()

    @Test
    fun `MvelAction 실행 - 예외 없이 완료`() {
        // MvelAction passes an immutable copy of facts to MVEL, so mutations in the script
        // don't propagate back to Facts. The action should execute without throwing.
        val action = MvelAction("discount = true")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
    }

    @Test
    fun `MvelAction 실행 - 계산식 예외 없이 완료`() {
        val action = MvelAction("result = amount * 2")
        val facts = Facts.of("amount" to 500)
        action.execute(facts)
    }

    @Test
    fun `MvelAction 실행 - 문자열 식 예외 없이 완료`() {
        val action = MvelAction("upper = name.toUpperCase()")
        val facts = Facts.of("name" to "alice")
        action.execute(facts)
    }

    @Test
    fun `MvelAction equals and hashCode`() {
        val expr = "discount = true"
        val a1 = MvelAction(expr)
        val a2 = MvelAction(expr)
        (a1 == a2).shouldBeTrue()
        (a1.hashCode() == a2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `MvelAction toString 표현`() {
        val action = MvelAction("x = 1")
        action.toString() shouldBeEqualTo "MvelAction(expression='x = 1')"
    }

    @Test
    fun `mvelActionOf 팩토리 함수`() {
        val action = mvelActionOf("result = x * 2")
        val facts = Facts.of("x" to 5)
        action.execute(facts)
        // MvelAction passes a copy; result not reflected back to facts
    }

    @Test
    fun `잘못된 MVEL 액션은 RuleException 발생`() {
        val action = MvelAction("@#\$invalid!!")
        val facts = Facts.empty()
        assertThrows<RuleException> {
            action.execute(facts)
        }
    }
}
