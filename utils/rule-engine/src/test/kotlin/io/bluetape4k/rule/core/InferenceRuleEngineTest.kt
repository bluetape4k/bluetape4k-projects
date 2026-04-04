package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class InferenceRuleEngineTest {

    companion object: KLogging()

    @Test
    fun `InferenceRuleEngine 기본 동작`() {
        val engine = InferenceRuleEngine()
        val rule = rule {
            name = "countDown"
            condition { facts -> facts.get<Int>("count")!! > 0 }
            action { facts ->
                val current = facts.get<Int>("count")!!
                facts["count"] = current - 1
            }
        }

        val facts = Facts.of("count" to 3)
        engine.fire(ruleSetOf(rule), facts)
        facts.get<Int>("count") shouldBeEqualTo 0
    }

    @Test
    fun `InferenceRuleEngine 조건 만족하는 Rule이 없으면 종료`() {
        val engine = InferenceRuleEngine()
        val rule = rule {
            name = "neverMatch"
            condition { false }
            action { facts -> facts["executed"] = true }
        }

        val facts = Facts.empty()
        engine.fire(ruleSetOf(rule), facts)
        facts.isEmpty().shouldBeTrue()
    }

    @Test
    fun `InferenceRuleEngine check 메서드 동작`() {
        val engine = InferenceRuleEngine()
        val rule = rule {
            name = "trueRule"
            condition { true }
            action { }
        }

        val result = engine.check(ruleSetOf(rule), Facts.empty())
        result[rule] shouldBeEqualTo true
    }

    @Test
    fun `InferenceRuleEngine 여러 Rule 순차 반복 실행`() {
        val engine = InferenceRuleEngine()
        val executionCount = mutableMapOf("count" to 0)

        val rule = rule {
            name = "incrementer"
            condition { facts -> facts.get<Int>("value")!! < 5 }
            action { facts ->
                val current = facts.get<Int>("value")!!
                facts["value"] = current + 1
                executionCount["count"] = executionCount["count"]!! + 1
            }
        }

        val facts = Facts.of("value" to 0)
        engine.fire(ruleSetOf(rule), facts)
        facts.get<Int>("value") shouldBeEqualTo 5
        executionCount["count"] shouldBeEqualTo 5
    }
}
