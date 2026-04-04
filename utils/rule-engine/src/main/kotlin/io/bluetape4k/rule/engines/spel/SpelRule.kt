package io.bluetape4k.rule.engines.spel

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.Condition
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.core.AbstractRule
import io.bluetape4k.support.requireNotBlank
import org.springframework.expression.BeanResolver
import org.springframework.expression.ParserContext
import java.util.*

/**
 * Spring Expression Language(SpEL)로 정의된 Rule입니다.
 *
 * ```kotlin
 * val rule = SpelRule(name = "discount")
 *     .whenever("#amount > 1000")
 *     .then("#discount = true")
 * ```
 */
class SpelRule private constructor(
    name: String,
    description: String,
    priority: Int,
): AbstractRule(name, description, priority) {

    companion object: KLogging() {
        private const val serialVersionUID = 1L

        @JvmStatic
        @JvmOverloads
        operator fun invoke(
            name: String = DEFAULT_RULE_NAME,
            description: String = DEFAULT_RULE_DESCRIPTION,
            priority: Int = DEFAULT_RULE_PRIORITY,
        ): SpelRule {
            name.requireNotBlank("name")
            return SpelRule(name, description, priority)
        }
    }

    private var condition: Condition = Condition.FALSE
    private val actions = LinkedList<SpelAction>()

    /**
     * SpEL 표현식으로 조건을 설정합니다.
     */
    @JvmOverloads
    fun whenever(
        expression: String,
        parserContext: ParserContext? = null,
        beanResolver: BeanResolver? = null,
    ) = apply {
        log.debug { "Set rule condition. expression=$expression" }
        this.condition = SpelCondition(expression, parserContext, beanResolver)
    }

    /**
     * [SpelCondition]으로 조건을 설정합니다.
     */
    fun whenever(condition: SpelCondition) = apply {
        log.debug { "Set rule condition. condition=$condition" }
        this.condition = condition
    }

    /**
     * SpEL 표현식으로 액션을 추가합니다.
     */
    @JvmOverloads
    fun then(
        expression: String,
        parserContext: ParserContext? = null,
        beanResolver: BeanResolver? = null,
    ) = apply {
        expression.requireNotBlank("expression")
        log.debug { "Add rule action. expression=$expression" }
        actions.add(SpelAction(expression, parserContext, beanResolver))
    }

    /**
     * [SpelAction]을 추가합니다.
     */
    fun then(action: SpelAction) = apply {
        log.debug { "Add rule action. action=$action" }
        actions.add(action)
    }

    override fun evaluate(facts: Facts): Boolean {
        return condition.evaluate(facts)
    }

    override fun execute(facts: Facts) {
        actions.forEach { action ->
            log.debug { "Execute action '$action' with facts=$facts" }
            action.execute(facts)
        }
    }
}
