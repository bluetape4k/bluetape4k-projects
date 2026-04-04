package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.SuspendAction
import io.bluetape4k.rule.api.SuspendCondition
import io.bluetape4k.rule.api.SuspendRule
import java.util.*

/**
 * [SuspendCondition]과 [SuspendAction] 목록을 가지는 SuspendRule 구현체입니다.
 * 액션은 순차적으로 실행됩니다.
 *
 * ```kotlin
 * val rule = DefaultSuspendRule(
 *     name = "asyncRule",
 *     priority = 1,
 *     condition = SuspendCondition { facts -> facts.get<Int>("value")!! > 0 },
 *     actions = listOf(SuspendAction { facts -> facts["processed"] = true })
 * )
 * val facts = Facts.of("value" to 5)
 * rule.evaluate(facts) // true
 * rule.execute(facts)
 * facts.get<Boolean>("processed") // true
 * ```
 *
 * @property condition 비동기 조건
 * @property actions 비동기 액션 목록
 */
open class DefaultSuspendRule(
    override val name: String = DEFAULT_RULE_NAME,
    override val description: String = DEFAULT_RULE_DESCRIPTION,
    override val priority: Int = DEFAULT_RULE_PRIORITY,
    val condition: SuspendCondition = SuspendCondition.FALSE,
    val actions: List<SuspendAction> = emptyList(),
): SuspendRule {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    override suspend fun evaluate(facts: Facts): Boolean {
        return condition.evaluate(facts)
    }

    override suspend fun execute(facts: Facts) {
        actions.forEach { action -> action.execute(facts) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuspendRule) return false
        return name == other.name
    }

    override fun hashCode(): Int = Objects.hash(name)

    override fun toString(): String = "SuspendRule(name='$name', priority=$priority, description='$description')"
}
