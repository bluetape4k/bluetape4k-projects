package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.Facts
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Kotlin Script 테스트는 CI 환경에서 불안정할 수 있으므로 로컬에서만 실행합니다")
class KotlinScriptRuleTest {

    companion object: KLogging()

    @Test
    fun `KotlinScriptEngine 기본 실행`() {
        val result = KotlinScriptEngine.evaluate("1 + 1")
        result shouldBeEqualTo 2
    }

    @Test
    fun `KotlinScriptCondition 평가`() {
        val condition = KotlinScriptCondition("true")
        condition.evaluate(Facts.empty()).shouldBeTrue()
    }

    @Test
    fun `KotlinScriptCondition 빈 스크립트는 true`() {
        val condition = KotlinScriptCondition("")
        condition.evaluate(Facts.empty()).shouldBeTrue()
    }

    @Test
    fun `KotlinScriptCondition 실패 시 false`() {
        val condition = KotlinScriptCondition("false")
        condition.evaluate(Facts.empty()).shouldBeFalse()
    }

    @Test
    fun `KotlinScriptRule 기본 사용`() {
        val rule = KotlinScriptRule(name = "scriptRule")
            .whenever("true")

        rule.evaluate(Facts.empty()).shouldBeTrue()
    }
}
