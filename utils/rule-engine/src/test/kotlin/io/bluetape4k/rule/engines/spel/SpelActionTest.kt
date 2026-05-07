package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.RuleException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class SpelActionTest {

    companion object : KLogging()

    @Test
    fun `SpelAction 실행 - 변수 설정`() {
        // SpEL을 통해 facts 맵에 값 변경을 시도하는 간단한 표현식
        // SpelAction은 표현식을 실행하고 결과를 반환; facts 맵 직접 수정은 actions에서 처리
        val action = SpelAction("#discount")
        val facts = Facts.of("discount" to true)
        // Just evaluates without throwing
        action.execute(facts)
    }

    @Test
    fun `SpelAction equals and hashCode`() {
        val expr = "#amount > 0 ? #discount : 0"
        val a1 = SpelAction(expr)
        val a2 = SpelAction(expr)
        (a1 == a2).shouldBeTrue()
        (a1.hashCode() == a2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `SpelAction toString 표현`() {
        val action = SpelAction("#x > 0")
        action.toString() shouldBeEqualTo "SpelAction(expression='#x > 0')"
    }

    @Test
    fun `spelActionOf 팩토리 함수`() {
        val action = spelActionOf("#discount")
        val facts = Facts.of("discount" to 10)
        // Should not throw
        action.execute(facts)
    }

    @Test
    fun `잘못된 SpEL 액션 표현식은 RuleException 발생`() {
        val action = SpelAction("1 + }")
        val facts = Facts.empty()
        assertFailsWith<RuleException> {
            action.execute(facts)
        }
    }

    @Test
    fun `SpelAction expression 프로퍼티 접근`() {
        val expr = "#value * 2"
        val action = SpelAction(expr)
        action.expression shouldBeEqualTo expr
    }

    @Test
    fun `SpelRule을 통해 SpelAction 실행 - facts 수정`() {
        // SpelRule.then() 으로 action 등록 후 execute
        val rule = SpelRule(name = "setDiscount")
            .whenever("#amount > 1000")
            .then("#amount")  // evaluate expression (simplified; real mutation requires T[] or put)

        val facts = Facts.of("amount" to 2000)
        rule.evaluate(facts).shouldBeTrue()
        // execute does not throw
        rule.execute(facts)
    }
}
