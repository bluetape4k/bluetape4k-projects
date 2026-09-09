package io.bluetape4k.rule.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.engines.groovy.GroovyAction
import io.bluetape4k.rule.engines.groovy.GroovyCondition
import io.bluetape4k.rule.engines.groovy.GroovyRule
import io.bluetape4k.rule.engines.janino.JaninoAction
import io.bluetape4k.rule.engines.janino.JaninoCondition
import io.bluetape4k.rule.engines.janino.JaninoRule
import io.bluetape4k.rule.engines.kotlinscript.KotlinScriptAction
import io.bluetape4k.rule.engines.kotlinscript.KotlinScriptCondition
import io.bluetape4k.rule.engines.kotlinscript.KotlinScriptRule
import io.bluetape4k.rule.engines.mvel2.MvelAction
import io.bluetape4k.rule.engines.mvel2.MvelCondition
import io.bluetape4k.rule.engines.mvel2.MvelRule
import io.bluetape4k.rule.engines.spel.SpelAction
import io.bluetape4k.rule.engines.spel.SpelCondition
import io.bluetape4k.rule.engines.spel.SpelRule
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class RuleScriptLoggingRedactionTest {

    @Test
    fun `all script engine definition logs summarize source`() {
        val source = "source-rule-definition-secret"
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val previousLevel = rootLogger.level

        InMemoryLogbackAppender("root").use { appender ->
            try {
                rootLogger.level = Level.TRACE

                GroovyRule(name = "groovy")
                    .whenever(source)
                    .then(source)
                JaninoRule(name = "janino")
                    .whenever(source)
                    .then(source)
                KotlinScriptRule(name = "kotlin-script")
                    .whenever(source)
                    .then(source)
                MvelRule(name = "mvel")
                    .whenever(source)
                    .then(source)
                SpelRule(name = "spel")
                    .whenever(source)
                    .then(source)

                val messages = appender.messages.joinToString("\n")
                messages shouldNotContain source
                messages shouldContain "sourceLength=${source.length}"
            } finally {
                rootLogger.level = previousLevel
            }
        }
    }

    @Test
    fun `all script engine failures omit source payload`() {
        val source = "source-rule-failure-secret"
        val facts = Facts.empty()
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val previousLevel = rootLogger.level

        InMemoryLogbackAppender("root").use { appender ->
            try {
                rootLogger.level = Level.TRACE

                listOf<() -> Unit>(
                    { GroovyCondition(source).evaluate(facts) },
                    { JaninoCondition(source).evaluate(facts) },
                    { KotlinScriptCondition(source).evaluate(facts) },
                    { MvelCondition(source).evaluate(facts) },
                    { SpelCondition(source).evaluate(facts) },
                    { GroovyAction(source).execute(facts) },
                    { JaninoAction(source).execute(facts) },
                    { KotlinScriptAction(source).execute(facts) },
                    { MvelAction(source).execute(facts) },
                    { SpelAction(source).execute(facts) },
                ).forEach { runCatching { it() } }

                val messages = appender.messages.joinToString("\n")
                messages shouldNotContain source
                messages shouldContain "sourceLength=${source.length}"
            } finally {
                rootLogger.level = previousLevel
            }
        }
    }
}
