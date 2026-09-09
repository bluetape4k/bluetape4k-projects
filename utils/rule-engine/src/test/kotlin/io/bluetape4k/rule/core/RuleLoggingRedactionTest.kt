package io.bluetape4k.rule.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.rule.annotation.Action as ActionAnnotation
import io.bluetape4k.rule.annotation.Condition as ConditionAnnotation
import io.bluetape4k.rule.annotation.Fact as FactAnnotation
import io.bluetape4k.rule.annotation.Rule as RuleAnnotation
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.ruleSetOf
import io.bluetape4k.rule.api.suspendRuleSetOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class RuleLoggingRedactionTest {

    @RuleAnnotation(name = "sensitiveProxyRule")
    class SensitiveProxyRule(
        private val expectedToken: String,
    ) {
        var executionCount: Int = 0

        @ConditionAnnotation
        fun matches(@FactAnnotation("token") token: String): Boolean = token == expectedToken

        @ActionAnnotation
        fun record(@FactAnnotation("token") token: String) {
            token shouldBeEqualTo expectedToken
            executionCount++
        }
    }

    @Test
    fun `동기 비동기 proxy 규칙 로그는 Facts와 호출 인자 payload를 노출하지 않는다`() = runTest {
        val logger = LoggerFactory.getLogger("io.bluetape4k.rule") as Logger
        val previousLevel = logger.level
        val secretToken = "rule-log-secret-token"

        InMemoryLogbackAppender("root").use { appender ->
            try {
                logger.level = Level.TRACE
                val facts = Facts.of("token" to secretToken)

                val proxyTarget = SensitiveProxyRule(secretToken)
                val proxyRule = proxyTarget.asRule()
                proxyRule.evaluate(facts).shouldBeTrue()
                proxyRule.execute(facts)
                proxyTarget.executionCount shouldBeEqualTo 1

                val syncExecutions = mutableListOf<String>()
                val syncRule = rule {
                    name = "sensitiveSyncRule"
                    condition { it.get<String>("token") == secretToken }
                    action { syncExecutions += "executed" }
                }
                DefaultRuleEngine().fire(ruleSetOf(syncRule), facts)

                val asyncExecutions = mutableListOf<String>()
                val asyncRule = suspendRule {
                    name = "sensitiveAsyncRule"
                    condition { it.get<String>("token") == secretToken }
                    action { asyncExecutions += "executed" }
                }
                DefaultSuspendRuleEngine().fire(suspendRuleSetOf(asyncRule), facts)

                syncExecutions shouldBeEqualTo listOf("executed")
                asyncExecutions shouldBeEqualTo listOf("executed")

                val messages = appender.messages.joinToString("\n")
                messages shouldContain "factCount=1"
                messages shouldNotContain secretToken
            } finally {
                logger.level = previousLevel
            }
        }
    }
}
