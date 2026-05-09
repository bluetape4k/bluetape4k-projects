package io.bluetape4k.rule.exception

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class RuleExceptionTest {

    companion object: KLogging()

    @Test
    fun `RuleException 기본 생성자`() {
        val ex = RuleException()
        ex.message.shouldBeNull()
        ex.cause.shouldBeNull()
    }

    @Test
    fun `RuleException 메시지 생성자`() {
        val ex = RuleException("테스트 오류")
        ex.message shouldBeEqualTo "테스트 오류"
    }

    @Test
    fun `RuleException 메시지+원인 생성자`() {
        val cause = RuntimeException("원인")
        val ex = RuleException("테스트 오류", cause)
        ex.message shouldBeEqualTo "테스트 오류"
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `RuleException 원인만 생성자`() {
        val cause = RuntimeException("원인")
        val ex = RuleException(cause)
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `InvalidRuleDefinitionException 생성`() {
        val ex = InvalidRuleDefinitionException("잘못된 Rule 정의")
        ex.message shouldBeEqualTo "잘못된 Rule 정의"
        (ex as? RuleException).shouldNotBeNull()
    }

    @Test
    fun `NoSuchFactException 생성 및 missingFact 프로퍼티`() {
        val ex = NoSuchFactException("age Fact 누락", "age")
        ex.message shouldBeEqualTo "age Fact 누락"
        ex.missingFact shouldBeEqualTo "age"
        (ex as? RuleException).shouldNotBeNull()
    }
}
