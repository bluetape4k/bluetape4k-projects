package io.bluetape4k.rule.engines.mvel2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class MvelActionTest {

    companion object : KLogging()

    @Test
    fun `MvelAction 실행 - 읽기 전용 표현식 예외 없이 완료`() {
        // MvelAction passes a read-only copy of facts to MVEL.
        // Read-only expressions (no assignment) execute successfully.
        val action = MvelAction("amount > 0")
        val facts = Facts.of("amount" to 1500)
        action.execute(facts)
    }

    @Test
    fun `MvelAction 실행 - 산술 읽기 표현식 예외 없이 완료`() {
        val action = MvelAction("amount * 2")
        val facts = Facts.of("amount" to 500)
        action.execute(facts)
    }

    @Test
    fun `MvelAction 실행 - 문자열 읽기 표현식 예외 없이 완료`() {
        val action = MvelAction("name.toUpperCase()")
        val facts = Facts.of("name" to "alice")
        action.execute(facts)
    }

    @Test
    fun `MvelAction equals and hashCode`() {
        val expr = "amount > 0"
        val a1 = MvelAction(expr)
        val a2 = MvelAction(expr)
        (a1 == a2).shouldBeTrue()
        (a1.hashCode() == a2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `MvelAction toString 표현`() {
        val action = MvelAction("x * 2")
        action.toString() shouldBeEqualTo "MvelAction(expression='x * 2')"
    }

    @Test
    fun `mvelActionOf 팩토리 함수`() {
        // Read-only expression — no assignment, so no UnsupportedOperationException
        val action = mvelActionOf("x * 2")
        val facts = Facts.of("x" to 5)
        action.execute(facts)
    }

    @Test
    fun `잘못된 MVEL 액션은 RuleException 발생`() {
        val action = MvelAction("@#\$invalid!!")
        val facts = Facts.empty()
        assertFailsWith<RuleException> {
            action.execute(facts)
        }
    }
}
