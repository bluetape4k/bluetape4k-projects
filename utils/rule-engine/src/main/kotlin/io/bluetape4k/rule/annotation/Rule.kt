package io.bluetape4k.rule.annotation

import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import java.lang.annotation.Inherited

/**
 * Rule을 정의하는 어노테이션입니다.
 * 이 어노테이션이 적용된 클래스는 [io.bluetape4k.rule.core.RuleProxy]를 통해 [io.bluetape4k.rule.api.Rule]로 변환됩니다.
 *
 * ```kotlin
 * @Rule(name = "ageCheck", description = "성인 확인")
 * class AgeCheckRule {
 *     @Condition
 *     fun isAdult(facts: Facts): Boolean = facts.get<Int>("age")!! >= 18
 *
 *     @Action
 *     fun allow(facts: Facts) { facts["allowed"] = true }
 * }
 * ```
 *
 * @property name Rule 이름
 * @property description Rule 설명
 * @property priority Rule 우선순위
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@MustBeDocumented
annotation class Rule(
    val name: String = DEFAULT_RULE_NAME,
    val description: String = DEFAULT_RULE_DESCRIPTION,
    val priority: Int = DEFAULT_RULE_PRIORITY,
)
