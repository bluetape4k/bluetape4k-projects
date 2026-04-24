package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.api.RuleEngineConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RuleEngineConfigTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 생성`() {
        val config = RuleEngineConfig()
        config.skipOnFirstAppliedRule.shouldBeFalse()
        config.skipOnFirstFailedRule.shouldBeFalse()
        config.skipOnFirstNonTriggeredRule.shouldBeFalse()
        config.priorityThreshold shouldBeEqualTo RuleEngineConfig.DEFAULT_PRIORITY_THRESHOLD
    }

    @Test
    fun `DEFAULT 상수 기본값 확인`() {
        val config = RuleEngineConfig.DEFAULT
        config.skipOnFirstAppliedRule.shouldBeFalse()
        config.skipOnFirstFailedRule.shouldBeFalse()
        config.skipOnFirstNonTriggeredRule.shouldBeFalse()
        config.priorityThreshold shouldBeEqualTo Int.MAX_VALUE
    }

    @Test
    fun `복사본은 동일값`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true, priorityThreshold = 100)
        val copy = config.copy()
        config shouldBeEqualTo copy
    }

    @Test
    fun `음수 priorityThreshold 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            RuleEngineConfig(priorityThreshold = -1)
        }
    }

    @Test
    fun `0 priorityThreshold 허용`() {
        val config = RuleEngineConfig(priorityThreshold = 0)
        config.priorityThreshold shouldBeEqualTo 0
    }

    @Test
    fun `skipOnFirstAppliedRule 설정`() {
        val config = RuleEngineConfig(skipOnFirstAppliedRule = true)
        config.skipOnFirstAppliedRule shouldBeEqualTo true
    }

    @Test
    fun `skipOnFirstFailedRule 설정`() {
        val config = RuleEngineConfig(skipOnFirstFailedRule = true)
        config.skipOnFirstFailedRule shouldBeEqualTo true
    }

    @Test
    fun `skipOnFirstNonTriggeredRule 설정`() {
        val config = RuleEngineConfig(skipOnFirstNonTriggeredRule = true)
        config.skipOnFirstNonTriggeredRule shouldBeEqualTo true
    }
}

