package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import java.util.*

/**
 * [Rule]의 최상위 추상화 클래스입니다.
 * 이름(name)을 기준으로 equals/hashCode를 구현합니다.
 *
 * ```kotlin
 * class MyRule : AbstractRule(name = "myRule", priority = 1) {
 *     override fun evaluate(facts: Facts): Boolean = facts.get<Int>("score")!! >= 60
 *     override fun execute(facts: Facts) { facts["passed"] = true }
 * }
 *
 * val rule = MyRule()
 * val facts = Facts.of("score" to 80)
 * rule.evaluate(facts) // true
 * rule.execute(facts)
 * facts.get<Boolean>("passed") // true
 * ```
 *
 * @property name Rule 이름
 * @property description Rule 설명
 * @property priority Rule 우선순위
 */
abstract class AbstractRule(
    override val name: String = DEFAULT_RULE_NAME,
    override val description: String = DEFAULT_RULE_DESCRIPTION,
    override val priority: Int = DEFAULT_RULE_PRIORITY,
): Rule {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override fun evaluate(facts: Facts): Boolean = false

    override fun execute(facts: Facts) {
        // No operation
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rule) return false
        return name == other.name
    }

    override fun hashCode(): Int = Objects.hash(name)

    override fun toString(): String = "Rule(name='$name', priority=$priority, description='$description')"
}
